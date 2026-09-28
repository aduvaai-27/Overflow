package com.orderflow.db;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_FILE = "orderflow.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private DatabaseConnection() { }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);

                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }

                initializeSchema();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not connect to OrderFlow database.", e);
        }
        return connection;
    }

    private static void initializeSchema() {
        try {
            String sql = readSchemaFile();
            String withoutComments = sql.replaceAll("(?m)^\\s*--.*$", "");
            java.util.List<String> creates = new java.util.ArrayList<>();
            java.util.List<String> seeds = new java.util.ArrayList<>();
            for (String rawStatement : withoutComments.split(";")) {
                String statement = rawStatement.trim();
                if (statement.isEmpty()) continue;
                (statement.regionMatches(true, 0, "INSERT", 0, 6) ? seeds : creates).add(statement);
            }

            runAll(creates);
            runMigrations();
            runAll(seeds);
            backfillProductSuppliers();
            System.out.println("OrderFlow database created and seeded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runAll(java.util.List<String> statements) {
        for (String statement : statements) {
            try (Statement st = connection.createStatement()) {
                st.execute(statement);
            } catch (SQLException e) {
                System.err.println("Schema statement failed: " + e.getMessage());
            }
        }
    }

    private static void backfillProductSuppliers() {
        try (Statement st = connection.createStatement()) {
            st.execute("UPDATE products SET supplier_id = " +
                    "(SELECT MIN(cs.supplier_id) FROM category_suppliers cs WHERE cs.category_id = products.category_id) " +
                    "WHERE supplier_id IS NULL");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void runMigrations() {
        ensureColumn("capital_transactions", "reason", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("order_items", "supplier_id", "INTEGER");
        ensureColumn("products", "supplier_id", "INTEGER");
        migrateLegacyCategorySupplier();
    }

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
                    return;
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

    private static String readSchemaFile() throws IOException {
        Path devPath = Paths.get("database", "schema.sql");
        if (Files.exists(devPath)) {
            return Files.readString(devPath);
        }

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
