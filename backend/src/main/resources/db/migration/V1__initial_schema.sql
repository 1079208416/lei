CREATE TABLE app_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(24) NOT NULL,
    merchant_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_account_phone ON app_account (phone);

CREATE TABLE merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(40) NOT NULL,
    cover_url VARCHAR(500),
    logo_url VARCHAR(500),
    address VARCHAR(240) NOT NULL,
    longitude DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL,
    rating DECIMAL(3, 1) NOT NULL DEFAULT 4.8,
    monthly_sales INT NOT NULL DEFAULT 0,
    min_order_amount INT NOT NULL DEFAULT 0,
    delivery_fee INT NOT NULL DEFAULT 0,
    packing_fee INT NOT NULL DEFAULT 0,
    eta_minutes INT NOT NULL DEFAULT 30,
    notice VARCHAR(500),
    open BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    price INT NOT NULL,
    original_price INT NULL,
    sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_sales INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE product_option_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE product_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    price_delta INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    label VARCHAR(40),
    contact_name VARCHAR(80) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    address VARCHAR(240) NOT NULL,
    detail VARCHAR(240),
    longitude DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    option_ids VARCHAR(240),
    options_snapshot VARCHAR(500),
    unit_price INT NOT NULL,
    cart_key VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_cart_key ON cart_item (cart_key);

CREATE TABLE food_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    account_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    merchant_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    refund_status VARCHAR(24) NOT NULL,
    address_snapshot VARCHAR(500) NOT NULL,
    contact_name VARCHAR(80) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    item_amount INT NOT NULL,
    packing_fee INT NOT NULL,
    delivery_fee INT NOT NULL,
    total_amount INT NOT NULL,
    remark VARCHAR(500),
    rider_id BIGINT NULL,
    cancel_reason VARCHAR(500),
    estimated_delivery_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    accepted_at TIMESTAMP NULL,
    ready_at TIMESTAMP NULL,
    picked_up_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_order_no ON food_order (order_no);
CREATE INDEX idx_order_user ON food_order (account_id, created_at);
CREATE INDEX idx_order_merchant ON food_order (merchant_id, status, created_at);
CREATE INDEX idx_order_status_expire ON food_order (status, created_at);

CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    product_image VARCHAR(500),
    options_snapshot VARCHAR(500),
    unit_price INT NOT NULL,
    quantity INT NOT NULL,
    total_price INT NOT NULL
);

CREATE INDEX idx_order_item_order ON order_item (order_id);

CREATE TABLE order_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    note VARCHAR(500),
    operator_role VARCHAR(24),
    operator_id BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_status_log_order ON order_status_log (order_id, created_at);

CREATE TABLE delivery_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    rider_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    picked_up_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_delivery_order ON delivery_task (order_id);

CREATE TABLE refund_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL
);

CREATE INDEX idx_refund_order ON refund_request (order_id);
