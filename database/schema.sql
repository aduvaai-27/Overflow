-- ==========================================================
-- OrderFlow Database Schema (SQLite)
-- This file is executed automatically by DatabaseConnection.java
-- the first time the application runs (orderflow.db does not exist yet)
-- ==========================================================

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name     TEXT,
    role          TEXT NOT NULL DEFAULT 'Staff'      -- Admin / Staff
);

CREATE TABLE IF NOT EXISTS categories (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- Every product row is owned by exactly ONE supplier - it is that company's
-- own listing (its own stock, its own price, its own SKU). Two companies
-- selling something with the same name (e.g. both sell a "Cotton T-Shirt")
-- are simply two separate product rows that happen to share a name; they
-- are never the same inventory line. supplier_id is nullable only for the
-- brief window before an owning supplier has been assigned on the Products page.
CREATE TABLE IF NOT EXISTS products (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT NOT NULL,
    sku            TEXT UNIQUE,
    category_id    INTEGER,
    supplier_id    INTEGER,
    purchase_price REAL NOT NULL DEFAULT 0,
    selling_price  REAL NOT NULL DEFAULT 0,
    stock_qty      INTEGER NOT NULL DEFAULT 0,
    min_stock      INTEGER NOT NULL DEFAULT 5,
    active         INTEGER NOT NULL DEFAULT 1,        -- 1 = active, 0 = deactivated
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS customers (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL,
    phone   TEXT,
    email   TEXT,
    address TEXT
);

CREATE TABLE IF NOT EXISTS orders (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id      INTEGER NOT NULL,
    order_date       TEXT NOT NULL,
    subtotal         REAL NOT NULL DEFAULT 0,
    discount         REAL NOT NULL DEFAULT 0,
    tax              REAL NOT NULL DEFAULT 0,
    delivery_charge  REAL NOT NULL DEFAULT 0,
    total            REAL NOT NULL DEFAULT 0,
    payment_method   TEXT NOT NULL DEFAULT 'COD',      -- COD / Card / Mobile Banking
    payment_status   TEXT NOT NULL DEFAULT 'Pending',  -- Pending / Paid
    order_status     TEXT NOT NULL DEFAULT 'Pending',  -- Pending/Confirmed/Shipped/Delivered/Cancelled
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id   INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity   INTEGER NOT NULL,
    unit_price REAL NOT NULL,
    line_total REAL NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id       INTEGER NOT NULL,
    change_qty       INTEGER NOT NULL,     -- positive = stock in, negative = stock out
    reason           TEXT NOT NULL,        -- Purchase / Order / Return / Adjustment
    transaction_date TEXT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL,
    phone   TEXT,
    email   TEXT,
    address TEXT
);

-- A category can be supplied by more than one company (and a company can
-- supply more than one category) - e.g. clothing might be bought from two
-- different garment suppliers. This is set from the Suppliers page.
CREATE TABLE IF NOT EXISTS category_suppliers (
    category_id INTEGER NOT NULL,
    supplier_id INTEGER NOT NULL,
    PRIMARY KEY (category_id, supplier_id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- A request to a supplier to restock a product. Moves through phases:
-- Requested -> Shipped -> Completed. Only when it reaches Completed does
-- the app automatically increase product stock and decrease capital
-- (the business pays the supplier's bill on completion, COD-style).
CREATE TABLE IF NOT EXISTS supplier_requests (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id    INTEGER NOT NULL,
    product_id     INTEGER NOT NULL,
    quantity       INTEGER NOT NULL,
    unit_cost      REAL NOT NULL,
    phase          TEXT NOT NULL DEFAULT 'Requested',  -- Requested / Shipped / Completed
    request_date   TEXT NOT NULL,
    completed_date TEXT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- A running ledger of the business's own money (capital), separate from
-- sales revenue. Positive amount = capital added; negative = capital spent.
-- Sale profit is added automatically when an order is paid; paying a
-- supplier on a completed restock request subtracts automatically.
CREATE TABLE IF NOT EXISTS capital_transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    amount           REAL NOT NULL,
    source           TEXT NOT NULL,   -- where the money came from/went to, e.g. "Sales Profit", "Bank Loan"
    reason           TEXT NOT NULL,   -- why, e.g. "Profit from Order #4", "Working capital top-up"
    transaction_date TEXT NOT NULL
);

-- ==========================================================
-- Seed data so the app is usable immediately after first run
-- Default login -> username: admin   password: admin123
-- ==========================================================

INSERT OR IGNORE INTO users (id, username, password_hash, full_name, role)
VALUES (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System Admin', 'Admin');

-- Suppliers are seeded before categories, since categories reference them.
INSERT OR IGNORE INTO suppliers (id, name, phone, email, address) VALUES
 (1, 'Dhaka Electronics Wholesale', '01910000001', 'sales@dhakaelectronics.example', 'Dhaka, Bangladesh'),
 (2, 'Khulna Garments Supply', '01910000002', 'info@khulnagarments.example', 'Khulna, Bangladesh'),
 (3, 'Rajshahi Textile Traders', '01910000003', 'info@rajshahitextile.example', 'Rajshahi, Bangladesh');

INSERT OR IGNORE INTO categories (id, name) VALUES
 (1, 'Electronics'),
 (2, 'Clothing'),
 (3, 'Groceries'),
 (4, 'Accessories');

-- Clothing is bought from two different companies, to show a category can have more than one supplier.
INSERT OR IGNORE INTO category_suppliers (category_id, supplier_id) VALUES
 (1, 1),
 (2, 2),
 (2, 3);

-- Note rows 3 and 8: both are named "Cotton T-Shirt" and both are Clothing,
-- but they are owned by two different suppliers, each with its own SKU,
-- price and stock - proof that the same product name can be sold by more
-- than one company as genuinely separate listings.
INSERT OR IGNORE INTO products (id, name, sku, category_id, supplier_id, purchase_price, selling_price, stock_qty, min_stock, active) VALUES
 (1, 'Wireless Mouse', 'ELEC-001', 1, 1, 400, 650, 25, 5, 1),
 (2, 'Bluetooth Headphone', 'ELEC-002', 1, 1, 1200, 1899, 15, 5, 1),
 (3, 'Cotton T-Shirt', 'CLTH-001', 2, 2, 250, 499, 40, 10, 1),
 (4, 'Denim Jeans', 'CLTH-002', 2, 2, 800, 1299, 20, 5, 1),
 (5, 'Instant Noodles (Pack)', 'GROC-001', 3, NULL, 30, 55, 100, 20, 1),
 (6, 'Leather Wallet', 'ACC-001', 4, NULL, 350, 699, 8, 5, 1),
 (7, 'Phone Charger Cable', 'ELEC-003', 1, 1, 90, 199, 3, 10, 1),
 (8, 'Cotton T-Shirt', 'CLTH-003', 2, 3, 270, 549, 18, 8, 1);

INSERT OR IGNORE INTO customers (id, name, phone, email, address) VALUES
 (1, 'Rahim Uddin', '01710000001', 'rahim@example.com', 'Khulna, Bangladesh'),
 (2, 'Karim Hossain', '01710000002', 'karim@example.com', 'Dhaka, Bangladesh'),
 (3, 'Fatema Akter', '01710000003', 'fatema@example.com', 'Rajshahi, Bangladesh');

INSERT OR IGNORE INTO capital_transactions (id, amount, source, reason, transaction_date) VALUES
 (1, 100000, 'Owner Investment', 'Opening capital', '2024-01-01T00:00:00');
