PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name     TEXT,
    role          TEXT NOT NULL DEFAULT 'Staff'
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
    active         INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (category_id) REFERENCES categories(id)
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
    payment_method   TEXT NOT NULL DEFAULT 'COD',
    payment_status   TEXT NOT NULL DEFAULT 'Pending',
    order_status     TEXT NOT NULL DEFAULT 'Pending',
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
    change_qty       INTEGER NOT NULL,
    reason           TEXT NOT NULL,
    transaction_date TEXT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS capital_transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    amount           REAL NOT NULL,
    source           TEXT NOT NULL,
    reason           TEXT NOT NULL DEFAULT '',
    transaction_date TEXT NOT NULL
);

INSERT OR IGNORE INTO users (id, username, password_hash, full_name, role)
VALUES (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System Admin', 'Admin');

INSERT OR IGNORE INTO categories (id, name) VALUES
 (1, 'Electronics'),
 (2, 'Clothing'),
 (3, 'Groceries'),
 (4, 'Accessories');

INSERT OR IGNORE INTO products (id, name, sku, category_id, purchase_price, selling_price, stock_qty, min_stock, active) VALUES
 (1, 'Wireless Mouse', 'ELEC-001', 1, 400, 650, 25, 5, 1),
 (2, 'Bluetooth Headphone', 'ELEC-002', 1, 1200, 1899, 15, 5, 1),
 (3, 'Cotton T-Shirt', 'CLTH-001', 2, 250, 499, 40, 10, 1),
 (4, 'Denim Jeans', 'CLTH-002', 2, 800, 1299, 20, 5, 1),
 (5, 'Instant Noodles (Pack)', 'GROC-001', 3, 30, 55, 100, 20, 1),
 (6, 'Leather Wallet', 'ACC-001', 4, 350, 699, 8, 5, 1),
 (7, 'Phone Charger Cable', 'ELEC-003', 1, 90, 199, 3, 10, 1);

INSERT OR IGNORE INTO customers (id, name, phone, email, address) VALUES
 (1, 'Rahim Uddin', '01710000001', 'rahim@example.com', 'Khulna, Bangladesh'),
 (2, 'Karim Hossain', '01710000002', 'karim@example.com', 'Dhaka, Bangladesh'),
 (3, 'Fatema Akter', '01710000003', 'fatema@example.com', 'Rajshahi, Bangladesh');

INSERT OR IGNORE INTO capital_transactions (id, amount, source, reason, transaction_date) VALUES
 (1, 100000, 'Owner Investment', 'Opening capital', '2024-01-01T00:00:00');
