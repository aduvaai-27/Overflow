package com.orderflow.db;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Single point of access to the SQLite database.
 *
 * Topic covered: "Relational Database with SQLite and JavaFX".
 * We use the sqlite-jdbc driver, so no separate database server needs to be
 * installed -- the whole database lives in one file: orderflow.db
 */
public class DatabaseConnection {

    private static final String DB_FILE = "orderflow.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    // Simple singleton connection - fine for a student desktop project.
    private static Connection connection;

    private DatabaseConnection() { }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);

                // Enforce foreign key constraints (SQLite disables them by default)
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }

                // Always (re)run schema.sql: every statement uses
                // CREATE TABLE IF NOT EXISTS / INSERT OR IGNORE, so this is
                // safe on an existing database too and lets new tables
                // (e.g. suppliers, capital_transactions) appear for
                // installs that were created before those features existed.
                initializeSchema();

                // CREATE TABLE IF NOT EXISTS won't add a *new column* to a
                // table that already exists from an older version of the
                // app, so any column added after the table itself existed
                // needs an explicit, safe-to-repeat migration here.
                runMigrations();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not connect to OrderFlow database.", e);
        }
        return connection;
    }

    /**
     * Runs database/schema.sql the very first time the app starts so that
     * tables and seed data (default admin login, sample products, etc.)
     * exist without the user having to set anything up manually.
     */
    private static void initializeSchema() {
        try {
            String sql = readSchemaFile();
            try (Statement st = connection.createStatement()) {
                // Strip full-line "--" comments first, then split on ";". Doing the
                // split without stripping comments is fragile: a semicolon inside a
                // comment (e.g. "-- do X; then Y") would be mistaken for the end of
                // a real SQL statement and corrupt everything that follows it.
                String withoutComments = sql.replaceAll("(?m)^\\s*--.*$", "");
                for (String rawStatement : withoutComments.split(";")) {
                    String statement = rawStatement.trim();
                    if (!statement.isEmpty()) {
                        st.execute(statement);
                    }
                }
            }
            System.out.println("OrderFlow database created and seeded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds columns that were introduced after a table already existed in
     * earlier versions of the app. Safe to run on every startup: each column
     * is only added if it isn't already there, so existing data is untouched.
     */
    private static void runMigrations() {
        ensureColumn("capital_transactions", "reason", "TEXT NOT NULL DEFAULT ''");
        migrateLegacyCategorySupplier();
    }

    /**
     * An older version of the app stored a single supplier directly on the
     * categories table. Categories can now have more than one supplier (see
     * the category_suppliers table), so this carries any old single-supplier
     * link over automatically the first time an upgraded database is opened.
     */
    private static void migrateLegacyCategorySupplier() {
        boolean hasLegacyColumn = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(categories)")) {
            while (rs.next()) {
                if ("supplier_id".equalsIgnoreCase(rs.getString("name"))) {
                    hasLegacyColumn = true;
                    break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        if (!hasLegacyColumn) return;

        try (Statement st = connection.createStatement()) {
            st.execute("INSERT OR IGNORE INTO category_suppliers (category_id, supplier_id) " +
                    "SELECT id, supplier_id FROM categories WHERE supplier_id IS NOT NULL");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ensureColumn(String table, String column, String definition) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return; // column already exists, nothing to do
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Migrated: added column " + column + " to " + table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads schema.sql either from the packaged resources (when running
     * from a built jar) or straight from the /database folder (when
     * running inside the IDE / with mvn javafx:run).
     */
    private static String readSchemaFile() throws IOException {
        // 1) Try the project's /database/schema.sql (typical during development)
        Path devPath = Paths.get("database", "schema.sql");
        if (Files.exists(devPath)) {
            return Files.readString(devPath);
        }

        // 2) Fall back to classpath resource (if it was copied into resources)
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
            if (in != null) {
                return new String(in.readAllBytes());
            }
        }

        throw new FileNotFoundException("schema.sql was not found in ./database or on the classpath.");
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
