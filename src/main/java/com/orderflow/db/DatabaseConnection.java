package com.orderflow.db;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
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
            try (Statement st = connection.createStatement()) {
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
