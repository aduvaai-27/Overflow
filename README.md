# OrderFlow

I built OrderFlow as my order & inventory management project for a small online shop. Below is what I have implemented, what I used to build it, and where everything is in the code.

---

## Features I have implemented

- I have implemented a Login screen, with the password stored as a SHA-256 hash in SQLite, not plain text.
- I have implemented full CRUD for Categories and Products, with a minimum-stock level per product. If stock falls to or below that level, I show the product as LOW STOCK in the table and on the Dashboard. Every product belongs to one **supplier**, chosen on the product form, and has a **Unique ID**; if I leave the ID blank, the app generates the next free one from the category (for example `GADG-004`). Because products are supplier-specific, the same item bought from two suppliers is two products, each with its own ID and price. Selecting a category shows a catalogue table of **Supplier, Product, Price and Unique ID**, and I can't delete a supplier that still has products.
- I have implemented a Suppliers module, where I can add suppliers, link them to the categories they supply, and send them restock requests. Each request moves through three phases: Requested → Shipped → Completed. When I mark one Completed, I automatically add the stock in and pay the supplier out of Capital.
- I have implemented a Capital ledger, separate from sales revenue. I update it automatically when an order gets paid (adds the profit) or when a supplier request gets completed (subtracts the cost), and I can also add or withdraw capital manually with a reason.
- I have implemented full CRUD and search for Customers, plus an order history view for each customer.
- I have implemented an Orders module with a cart-style "New Order" screen. I pick a customer, then pick a **supplier** and then one of *that supplier's* products (the list shows each product's Unique ID and price), and add it with a quantity. The cart shows Unique ID, product and supplier, and the app works out subtotal, 5% tax, and delivery charge. When I confirm the order, I check stock availability, save the order and its items (including which supplier each line came from, shown on the invoice), reduce stock, and log an inventory transaction — all inside one database transaction, so nothing gets half-saved if something fails.
- I have implemented order status tracking: Confirmed → Shipped → Delivered → Completed. I only allow a Cash-on-Delivery order to be marked Paid once it has actually been Delivered. I also allow cancelling an order before it ships, which automatically restores the stock and reverses the profit from Capital if it was already paid.
- I have implemented a Reports page where I pick a date range and get order count, revenue, average order value, and two bar charts: best-selling product by quantity (X axis = product, Y axis = units sold) and highest revenue product (X axis = product, Y axis = revenue in Tk).
- I have implemented a Dashboard as the home page, showing live counters for products, customers, orders, revenue, capital, pending COD orders, a live low-stock counter, my best customer, and a monthly profit chart. I also show a live USD-equivalent figure under Revenue and Capital, fetched from a public exchange-rate API.

---

## What I used, and where I used it (course topics)

- I used basic Java syntax — loops, arrays, conditionals, try/catch — throughout the project, but `util/PasswordUtil.java` and `util/DateUtil.java` are good small examples if you want to see it in isolation.
- I used `javac` to compile my `.java` files into bytecode, and the JVM to run that bytecode, instead of compiling straight to machine code the way `gcc` does for C. That's why the same OrderFlow build runs unmodified on any OS with a JVM installed.
- I used Git and version control to build this project in stages instead of one single commit — I committed the Capital feature first, then Suppliers, then Reports/charts, so the commit history actually reflects how the project grew.
- I used JavaFX for the entire GUI. I have one `.fxml` file per screen in `src/main/resources/com/orderflow/fxml/`, and a matching controller class in `controller/` that handles the events (every `onAction="#method"` in the fxml maps to a method in its controller).
- I used Java multithreading in `service/StockAlertMonitor.java` — it runs on its own background thread, checks for low-stock products every few seconds, and reports back to the JavaFX UI thread safely using `Platform.runLater()`.
- I used JavaFX's `Task` API for concurrent task management in `service/ExchangeRateService.java`, so the exchange-rate API call runs in the background and never freezes the UI.
- I used SQLite as my relational database. I used `db/DatabaseConnection.java` to open the connection and run `database/schema.sql` on first launch, and I used one DAO class per table in `dao/` (`ProductDAO`, `OrderDAO`, `CapitalDAO`, etc.) for all my SQL — including multi-step transactions like placing an order.
- I used JSON parsing and API response handling in `service/ExchangeRateService.java`. I call `open.er-api.com` over HTTPS with `java.net.http.HttpClient`, and I parse the JSON response with the `org.json` library to pull out the USD→BDT rate.

---

## Project structure

```
OrderFlow/
├── pom.xml                       my Maven build file — pulls in JavaFX, sqlite-jdbc, org.json
├── database/schema.sql           my table definitions + starter sample data
├── src/main/java/com/orderflow/
│   ├── Main.java                  application entry point
│   ├── model/                     plain data classes — Product, Order, Supplier, CapitalTransaction, etc.
│   ├── dao/                       one class per table, all my SQL lives here
│   ├── business/                  service layer I added between controllers and dao
│   ├── service/                   StockAlertMonitor and ExchangeRateService
│   ├── controller/                one controller per screen
│   └── util/                      PasswordUtil, AlertUtil, Session, DateUtil, TableColorUtil, ChartUtil
└── src/main/resources/com/orderflow/
    ├── fxml/                      one layout file per screen
    └── css/style.css              all my styling
```

---

## How I run it

I used Maven to manage all my dependencies, so to run it I just do:

```bash
cd OrderFlow
mvn clean javafx:run
```

The first time I run it, Maven downloads JavaFX, the SQLite driver, and the JSON library automatically, and the app creates `orderflow.db` with sample data already in it.

If I'm using an IDE instead (IntelliJ, Eclipse, etc.), I open the folder as a Maven project and run the `com.orderflow.Main` class.

Default login:

```
Username: admin
Password: admin123
```

---

## What I kept simple on purpose

- I used a fixed 5% tax rate and a flat delivery charge instead of making them configurable.
- I implemented a single Admin-style login instead of building out per-role permissions.
- I kept the supplier request flow to one fixed 3-step process instead of a configurable approval workflow.

I kept these simple on purpose, so the project stayed a manageable size while still covering everything I needed to for the assignment.
