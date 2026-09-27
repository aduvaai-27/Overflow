# OrderFlow — E-Commerce Order & Inventory Management System

A desktop Java application for managing products, stock, customers,
suppliers, capital and orders for a small online shop. Built as a student
project covering:

- C and Java Compilation; Introduction to Java Syntax
- Desktop GUI Development with JavaFX
- Java Multithreading and Concurrency
- Relational Database with SQLite and JavaFX
- JSON Parsing and API Response Handling with Java

---

## 1. What the app does

- **Login** with a hashed password (SHA-256) stored in SQLite.
- **Categories & Products** — full CRUD, with a minimum-stock level per
  product. Products below their minimum are flagged **LOW STOCK**.
- **Customers** — full CRUD with search. Double-click a customer (or use
  "View History") to see everything they've bought: what, when, how they
  paid, and the order/payment status of each order.
- **Orders** — a cart-style "New Order" screen: pick a customer, add
  products with quantities, the app calculates subtotal, 5% tax and a
  flat delivery charge, then confirms the order. Confirming an order:
  1. Checks stock is available for every item
  2. Saves the order + its line items
  3. Reduces stock for each product sold
  4. Writes an audit row to `inventory_transactions`

  All four of these happen inside **one database transaction** — if
  anything fails, everything is rolled back so stock and orders never
  go out of sync.
- **Invoice** — a simple printable invoice for any order.
- **Order status** — Confirmed → Shipped → Delivered → Completed.
  - A Cash-on-Delivery order's payment can **only** be marked Paid once
    it has actually been Delivered — never before.
  - An order can be **cancelled** as long as it hasn't shipped yet;
    cancelling returns the reserved stock automatically (and reverses
    the sale's profit from capital if it had already been paid).
- **Suppliers** — keep a list of the companies you buy stock from, and
  send them restock requests (product + quantity + unit cost). Each
  request moves through phases **Requested → Shipped → Completed**;
  reaching Completed automatically increases the product's stock and
  pays the supplier out of capital.
- **Capital** — a running ledger of the business's own money, kept
  separate from sales revenue. Profit from a paid order is added to it
  automatically, paying a supplier on a completed restock request
  subtracts from it automatically, and you can also add or withdraw
  capital manually (with a required reason) from the Capital page.
- **Reports** — pick a date range and see order count, revenue, average
  order value, and two bar charts: the highest-selling product by
  quantity and the highest revenue-generating product.
- **Dashboard** — live counters for products, customers, orders, revenue
  and capital, the current best-seller by quantity, a monthly profit
  (sell price − buy price, stock not counted) bar chart, and a live
  USD-equivalent figure under Total Revenue and Capital (fetched from a
  public exchange-rate API in the background).

---

## 2. Where each required topic lives in the code

| Topic | Where |
|---|---|
| Introduction to Java Syntax | Throughout — see especially `util/PasswordUtil.java` for basic loops/arrays/try-catch |
| C and Java Compilation | See section 5 below, and the comment at the top of `Main.java` |
| JavaFX GUI | `src/main/resources/com/orderflow/fxml/*.fxml` + matching classes in `controller/` |
| SQLite + JavaFX | `db/DatabaseConnection.java` and every class in `dao/` |
| Multithreading & Concurrency | `service/StockAlertMonitor.java` — a background thread scans for low stock every 8 seconds and safely reports back to the UI thread with `Platform.runLater()` |
| JSON Parsing & API Response Handling | `service/ExchangeRateService.java` — fetches the live USD→BDT rate from a public REST API (`open.er-api.com`) using `java.net.http.HttpClient`, parses the JSON with `org.json`, and runs it on a background `javafx.concurrent.Task` so the UI never freezes. It powers the small "≈ $X USD" figure under Total Revenue and Capital on the Dashboard (needs internet access; the Dashboard just shows Tk-only if the API can't be reached). |

---

## 3. Project structure

```
OrderFlow/
├── pom.xml                     Maven build file (JavaFX + sqlite-jdbc + org.json)
├── database/schema.sql         Table definitions + sample data
├── src/main/java/com/orderflow/
│   ├── Main.java                Application entry point
│   ├── model/                   Plain data classes (Product, Order, Supplier, CapitalTransaction, ...)
│   ├── dao/                     Database access classes (one per table)
│   ├── service/                 StockAlertMonitor + ExchangeRateService
│   ├── controller/               One controller per screen
│   └── util/                    PasswordUtil, AlertUtil, Session
└── src/main/resources/com/orderflow/
    ├── fxml/                    One FXML layout per screen
    └── css/style.css            App styling
```

---

## 4. How to run it

### Option A — Maven (recommended, needs internet the first time)

You need **JDK 17+** and **Maven** installed.

```bash
cd OrderFlow
mvn clean javafx:run
```

Maven will download JavaFX, the SQLite driver and the JSON library
automatically the first time you build.

### Option B — An IDE (IntelliJ IDEA / Eclipse / NetBeans)

1. Open the folder as a **Maven project** — the IDE will read `pom.xml`
   and download the dependencies for you.
2. Run the `com.orderflow.Main` class.

### First run

The very first time you run the app, `orderflow.db` (a single SQLite
file) is created automatically in the project folder and filled with
sample categories, products, customers, suppliers and an opening
capital balance, plus a default login:

```
Username: admin
Password: admin123
```

If you already had an older `orderflow.db` from before the Suppliers/
Capital features existed, it will be upgraded in place the next time
you run the app — the new tables are added automatically without
touching your existing data.

---

## 5. A note on "C and Java Compilation"

This topic is really about *how* your source code becomes a running
program, and Java does it differently from C:

- **C**: a compiler such as `gcc` translates your `.c` file directly
  into **machine code** for one specific processor and operating
  system. `gcc main.c -o main` on Linux produces a file that will
  *not* run on Windows without recompiling.
- **Java**: the `javac` compiler translates your `.java` file into
  **bytecode** (`.class` files) — an intermediate format that is not
  tied to any particular machine. The **Java Virtual Machine (JVM)**
  then reads that bytecode and executes it on whatever operating
  system it is installed on. That is why the exact same OrderFlow
  program runs unmodified on Windows, Linux or macOS, as long as a JVM
  is installed — this is Java's famous "write once, run anywhere".

You can see both compilers in action for a trivial example:

```bash
# C
gcc hello.c -o hello      # produces a native executable
./hello

# Java
javac Hello.java          # produces Hello.class (bytecode)
java Hello                # the JVM interprets/JIT-compiles and runs it
```

For OrderFlow specifically, compiling by hand (without Maven) would
look like this once you have the JavaFX SDK and the sqlite-jdbc `.jar`
on your classpath:

```bash
javac -cp "sqlite-jdbc.jar;json.jar;javafx-sdk/lib/*" \
      -d build $(find src/main/java -name "*.java")

java --module-path javafx-sdk/lib --add-modules javafx.controls,javafx.fxml \
     -cp "build;sqlite-jdbc.jar;json.jar" com.orderflow.Main
```

(Use `:` instead of `;` between paths on Linux/macOS.) In practice,
Maven's `javafx:run` command in Option A above does exactly this for
you, which is why it is the recommended way to run the project.

---

## 6. Known limitations (kept simple on purpose)

- Tax rate (5%) and delivery charge (Tk 60) are fixed constants rather
  than configurable settings.
- There is a single Admin-style login rather than fine-grained
  permissions per role.
- Supplier restock phases are a simple 3-step flow (Requested → Shipped
  → Completed) rather than a fully configurable approval workflow.

These were left out deliberately to keep the project at a manageable,
demonstrable size for a course assignment while still covering every
required topic with real, working code.
