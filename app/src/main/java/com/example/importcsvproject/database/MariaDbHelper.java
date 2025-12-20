package com.example.importcsvproject.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MariaDbHelper {
    private static final String IP = "10.0.2.2";
    private static final String PORT = "3306";
    private static final String DB_NAME = "smart_card_db";
    private static final String USER = "admin";
    private static final String PASS = "123456";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + IP + ":" + PORT + "/" + DB_NAME + "?useSSL=false&characterEncoding=UTF-8";

            conn = DriverManager.getConnection(url, USER, PASS);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}