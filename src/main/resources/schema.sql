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

CREATE TABLE IF NOT EXISTS products (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT NOT NULL,
    sku            TEXT UNIQUE,
    category_id    INTEGER,
    purchase_price REAL NOT NULL DEFAULT 0,
    selling_price  REAL NOT NULL DEFAULT 0,
    stock_qty      INTEGER NOT NULL DEFAULT 0,
    min_stock      INTEGER NOT NULL DEFAULT 5,
    active         INTEGER NOT NULL DEFAULT 1,        -- 1 = active, 0 = deactivated
    supplier_id    INTEGER,                           -- the company this product is bought from
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
    supplier_id INTEGER,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
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
 (1, 'Chattogram Tech Traders', '01920000001', 'sales@chattogramtech.example', 'Chattogram, Bangladesh'),
 (2, 'Sylhet Fashion House', '01920000002', 'info@sylhetfashion.example', 'Sylhet, Bangladesh'),
 (3, 'Barishal Cotton Mills', '01920000003', 'info@barishalcotton.example', 'Barishal, Bangladesh'),
 (4, 'Rangpur Agro Foods', '01920000004', 'orders@rangpuragro.example', 'Rangpur, Bangladesh');

INSERT OR IGNORE INTO categories (id, name) VALUES
 (1, 'Gadgets'),
 (2, 'Fashion'),
 (3, 'Food & Beverage'),
 (4, 'Lifestyle');

-- Which categories each supplier covers (the "Categories Supplied" ticks on the Suppliers page).
-- Fashion and Lifestyle are each covered by two companies, and every link has at least one product below.
INSERT OR IGNORE INTO category_suppliers (category_id, supplier_id) VALUES
 (1, 1),
 (2, 2),
 (2, 3),
 (3, 4),
 (4, 2),
 (4, 3);

-- Products are supplier-specific: each one is bought from exactly one supplier, so the same item from
-- two suppliers is two separate products with their own Unique ID and price (see the two Polo Shirts).
-- Unique IDs follow the same pattern the app generates: first 4 letters of the category + running number.
INSERT OR IGNORE INTO products (id, name, sku, category_id, supplier_id, purchase_price, selling_price, stock_qty, min_stock, active) VALUES
 (1, 'Wireless Keyboard', 'GADG-001', 1, 1, 900, 1450, 18, 5, 1),
 (2, 'Bluetooth Speaker', 'GADG-002', 1, 1, 1500, 2299, 12, 5, 1),
 (3, 'USB Power Bank', 'GADG-003', 1, 1, 700, 1150, 3, 10, 1),
 (4, 'Polo Shirt', 'FASH-001', 2, 2, 300, 599, 35, 10, 1),
 (5, 'Slim Fit Trousers', 'FASH-002', 2, 2, 700, 1199, 22, 5, 1),
 (6, 'Polo Shirt', 'FASH-003', 2, 3, 280, 549, 28, 10, 1),
 (7, 'Cotton Panjabi', 'FASH-004', 2, 3, 650, 1099, 15, 5, 1),
 (8, 'Basmati Rice (5 kg)', 'FOOD-001', 3, 4, 520, 640, 60, 15, 1),
 (9, 'Olive Oil (500 ml)', 'FOOD-002', 3, 4, 600, 780, 40, 10, 1),
 (10, 'Canvas Backpack', 'LIFE-001', 4, 3, 450, 899, 9, 5, 1),
 (11, 'Leather Wallet', 'LIFE-002', 4, 2, 350, 699, 8, 5, 1);

INSERT OR IGNORE INTO customers (id, name, phone, email, address) VALUES
 (1, 'Nasir Ahmed', '01810000001', 'nasir@example.com', 'Chattogram, Bangladesh'),
 (2, 'Sumaiya Rahman', '01810000002', 'sumaiya@example.com', 'Sylhet, Bangladesh'),
 (3, 'Tanvir Hasan', '01810000003', 'tanvir@example.com', 'Barishal, Bangladesh');

INSERT OR IGNORE INTO capital_transactions (id, amount, source, reason, transaction_date) VALUES
 (1, 150000, 'Owner Investment', 'Opening capital', '2024-01-01T00:00:00');
