package com.example.product.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler for the entire API.
 *
 * KEY CONCEPTS:
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * It intercepts exceptions thrown by ANY controller and converts them to
 * HTTP responses. Without this, unhandled exceptions become generic 500s
 * with potentially leaked stack traces.
 *
 * EXTENDING ResponseEntityExceptionHandler:
 * Spring MVC already handles many standard exceptions (MethodArgumentNotValid,
 * HttpRequestMethodNotSupported, etc.) via this base class. By extending it,
 * we inherit those handlers and only override the ones we want to customize.
 *
 * ProblemDetail (RFC 9457):
 * Spring Boot 3 supports this standard error format natively. ProblemDetail
 * provides: type, title, status, detail, instance — and allows custom
 * properties via setProperty(). This gives every error a consistent,
 * machine-readable structure.
 *
 * DESIGN: we map exception types to HTTP status codes:
 * - ResourceNotFoundException → 404
 * - MethodArgumentNotValidException → 422 (with field errors)
 * - IllegalArgumentException → 400
 * - Everything else → 500
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle resource-not-found (our custom exception) → 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        return problem;
    }

    /**
     * Handle bean validation failures → 422 with field-level error details.
     *
     * This OVERRIDES the default Spring handler (which returns 400). We return
     * 422 to distinguish "syntactically valid but semantically wrong" from
     * "malformed/unparseable" (400). This matches our Phase 1 design decision.
     *
     * The fieldErrors list lets API consumers map errors to specific form fields.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed");
        problem.setTitle("Validation Failed");

        // Extract field-level errors into a structured list
        List<Map<String, Object>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.<String, Object>of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null
                                ? error.getDefaultMessage() : "Invalid value",
                        "rejectedValue", error.getRejectedValue() != null
                                ? error.getRejectedValue() : "null"
                ))
                .toList();

        // Add field errors as a custom ProblemDetail property
        problem.setProperty("fieldErrors", fieldErrors);

        log.debug("Validation failed with {} errors", fieldErrors.size());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(problem);
    }

    /**
     * Handle illegal arguments (bad business logic input) → 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return problem;
    }

    /**
     * Catch-all for unexpected exceptions → 500.
     *
     * IMPORTANT: we log the full stack trace (for debugging) but return
     * a generic message to the client (for security — never leak internals).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllUncaught(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
