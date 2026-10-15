package com.example.product.exception;

/**
 * Thrown when a requested resource doesn't exist.
 *
 * This is a custom RuntimeException (unchecked). We throw it from the service
 * layer; the GlobalExceptionHandler catches it and returns a 404 response.
 *
 * WHY RuntimeException (unchecked) instead of checked Exception?
 * Spring's @Transactional rolls back by default only on unchecked exceptions.
 * Also, checked exceptions force every caller to handle or declare them,
 * cluttering the code for a condition (not found) that should simply
 * propagate to the error handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
