package com.enterprise.crm.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbScriptRunner {

    private final String url;
    private final String username;
    private final String password;

    public DbScriptRunner(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void runScript(String resourcePath) throws Exception {
        System.out.println("\n=======================================================");
        System.out.println("Running SQL script: " + resourcePath);
        System.out.println("=======================================================");

        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            // Try loading from classpath or file
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        }
        if (is == null) {
            throw new IllegalArgumentException("Script not found: " + resourcePath);
        }

        List<String> commands = parseScript(is);
        System.out.println("Parsed " + commands.size() + " commands from " + resourcePath);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false);
            int successCount = 0;
            int errorCount = 0;

            for (int i = 0; i < commands.size(); i++) {
                String cmd = commands.get(i).trim();
                if (cmd.isEmpty() || cmd.equalsIgnoreCase("COMMIT")) {
                    if (cmd.equalsIgnoreCase("COMMIT")) conn.commit();
                    continue;
                }

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(cmd);
                    successCount++;
                    // Print summary for important operations
                    String firstLine = cmd.split("\n")[0];
                    if (firstLine.length() > 60) firstLine = firstLine.substring(0, 60) + "...";
                    System.out.println("[" + (i + 1) + "/" + commands.size() + "] OK: " + firstLine);
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("[" + (i + 1) + "/" + commands.size() + "] FAILED: " + e.getMessage());
                    String snippet = cmd.length() > 120 ? cmd.substring(0, 120) + "..." : cmd;
                    System.err.println("   Statement: " + snippet);
                }
            }

            conn.commit();
            System.out.println("Completed " + resourcePath + " -> " + successCount + " succeeded, " + errorCount + " failed.");
        }
    }

    private List<String> parseScript(InputStream is) throws Exception {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inPlSql = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Skip full line comments
                if (trimmed.startsWith("--")) {
                    continue;
                }

                // Check for PL/SQL start
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("CREATE OR REPLACE PACKAGE") ||
                    upper.startsWith("CREATE OR REPLACE PROCEDURE") ||
                    upper.startsWith("CREATE OR REPLACE FUNCTION") ||
                    upper.startsWith("CREATE OR REPLACE TRIGGER") ||
                    upper.startsWith("BEGIN") ||
                    upper.startsWith("DECLARE")) {
                    inPlSql = true;
                }

                if (inPlSql) {
                    // In PL/SQL, delimiter is '/' on its own line
                    if (trimmed.equals("/")) {
                        if (current.length() > 0) {
                            commands.add(current.toString().trim());
                            current.setLength(0);
                        }
                        inPlSql = false;
                    } else {
                        current.append(line).append("\n");
                    }
                } else {
                    // Regular SQL statement delimited by ';'
                    if (trimmed.endsWith(";")) {
                        current.append(line.substring(0, line.lastIndexOf(';'))).append("\n");
                        commands.add(current.toString().trim());
                        current.setLength(0);
                    } else if (!trimmed.isEmpty()) {
                        current.append(line).append("\n");
                    }
                }
            }

            if (current.length() > 0 && !current.toString().trim().isEmpty()) {
                commands.add(current.toString().trim());
            }
        }

        return commands;
    }

    public static void main(String[] args) {
        String url = "jdbc:oracle:thin:@//db.freesql.com:1521/26ai_un3c1";
        String username = "MDRAZATECH_SCHEMA_GMAXC";
        String password = "8VTaH9$WQAOBMZ01T093TPJ2EET9JH";

        DbScriptRunner runner = new DbScriptRunner(url, username, password);

        try {
            System.out.println(">>> Starting Database Initialization on Oracle 26ai Cloud...");
            runner.runScript("/db/01_schema.sql");
            runner.runScript("/db/02_packages.sql");
            runner.runScript("/db/03_seed_data.sql");
            System.out.println("\n>>> ALL DATABASE OBJECTS & SEED DATA INITIALIZED SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
