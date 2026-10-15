-- =============================================================================
-- Sample data loaded on application startup.
--
-- Spring Boot executes this file AFTER Hibernate creates the schema
-- (because of spring.jpa.defer-datasource-initialization=true in application.yml).
--
-- This gives us products to work with immediately in Swagger UI and curl.
-- 5 products, 3 categories, 1 inactive, 1 out-of-stock.
-- =============================================================================

INSERT INTO products (name, description, price, category, stock_quantity, active, created_at, updated_at)
VALUES ('Wireless Keyboard', 'Bluetooth mechanical keyboard with RGB lighting', 49.99, 'Electronics', 150, TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO products (name, description, price, category, stock_quantity, active, created_at, updated_at)
VALUES ('Running Shoes', 'Lightweight trail running shoes', 89.95, 'Sports', 75, TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO products (name, description, price, category, stock_quantity, active, created_at, updated_at)
VALUES ('Coffee Maker', 'Programmable 12-cup drip coffee maker', 34.50, 'Home & Kitchen', 200, TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO products (name, description, price, category, stock_quantity, active, created_at, updated_at)
VALUES ('Java Programming Book', 'Effective Java, 3rd Edition by Joshua Bloch', 45.00, 'Books', 50, TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO products (name, description, price, category, stock_quantity, active, created_at, updated_at)
VALUES ('Desk Lamp', 'LED desk lamp with adjustable brightness', 28.75, 'Home & Kitchen', 0, FALSE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
