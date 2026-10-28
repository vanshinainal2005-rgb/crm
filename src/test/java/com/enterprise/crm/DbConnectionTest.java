package com.enterprise.crm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbConnectionTest {
    public static void main(String[] args) {
        String username = "VANSHI_NAINAL2005_SCHEMA_8S4O5";
        String password = "N92L864OIDTT1J2C7F27GwR97X#2IH";

        String[] urls = {
            "jdbc:oracle:thin:@//118.214.188.244:1521/26ai_un3c1",
            "jdbc:oracle:thin:@//db.freesql.com:1521/26ai_un3c1",
            "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=db.freesql.com)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=26ai_un3c1)))",
            "jdbc:oracle:thin:@tcps://db.freesql.com:2484/26ai_un3c1"
        };

        for (String url : urls) {
            System.out.println("Trying connection to: " + url);
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 'Connection Successful! Oracle Version: ' || BANNER_FULL FROM V$VERSION")) {
                if (rs.next()) {
                    System.out.println(">>> SUCCESS! " + rs.getString(1));
                    System.out.println(">>> Working URL: " + url);
                    return;
                }
            } catch (Exception e) {
                System.out.println("FAILED for " + url + " : " + e.getMessage());
            }
        }
    }
}
