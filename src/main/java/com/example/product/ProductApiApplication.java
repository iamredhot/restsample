package com.example.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @SpringBootApplication is a convenience annotation combining:
 *   - @Configuration     → this class can define @Bean methods
 *   - @EnableAutoConfiguration → Spring Boot auto-configures beans based on
 *                                classpath (e.g., sees H2 → configures DataSource)
 *   - @ComponentScan     → scans this package and sub-packages for @Component,
 *                           @Service, @Repository, @Controller, @RestController
 *
 * SpringApplication.run() bootstraps the entire application:
 *   1. Creates the ApplicationContext (the Spring IoC container)
 *   2. Scans for components and configurations
 *   3. Auto-configures beans (DataSource, EntityManager, DispatcherServlet, etc.)
 *   4. Starts embedded Tomcat on port 8080 (default)
 *   5. The app is ready to serve HTTP requests
 */
@SpringBootApplication
public class ProductApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApiApplication.class, args);
    }
}
