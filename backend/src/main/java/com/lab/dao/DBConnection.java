package com.lab.dao;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        return com.lab.util.DBConnection.getConnection();
    }

    public static boolean testConnection() {
        return com.lab.util.DBConnection.testConnection();
    }

    public static void shutdown() {
        com.lab.util.DBConnection.shutdown();
    }
}
