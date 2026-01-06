package com.dataGovernance.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnectionUtils {

    public static Connection getRDJCConnection(String user, String password, String schema) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", password);

        String url = "jdbc:kingbase8://172.20.82.67:15433/SHBDC?currentSchema=" + schema +
                "&characterEncoding=UTF8&client_encoding=utf8&serverTimezone=GMT%2B8";
        // 加载 人大金仓 驱动
        try {
            Class.forName("com.kingbase8.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Kingbase JDBC driver not found", e);
        }

        return DriverManager.getConnection(url, properties);
    }

    public static Connection getConnection(String user, String password, String schema) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", password);

        String url = "jdbc:kingbase8://172.20.82.116:15433/SHBDC?currentSchema=" + schema +
                "&characterEncoding=UTF8&client_encoding=utf8&serverTimezone=GMT%2B8";
        // 加载 人大金仓 驱动
        try {
            Class.forName("com.kingbase8.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Kingbase JDBC driver not found", e);
        }

        return DriverManager.getConnection(url, properties);
    }

    public static Connection getDMConnection(String user, String password, String schema) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", password);

        String url = "jdbc:kingbase8://172.20.82.68:15433/skyt_slxx_dghy_prod?currentSchema=" + schema +
                "&characterEncoding=UTF8&client_encoding=utf8&serverTimezone=GMT%2B8";
        // 加载 人大金仓 驱动
        try {
            Class.forName("com.kingbase8.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Kingbase JDBC driver not found", e);
        }

        return DriverManager.getConnection(url, properties);
    }

}
