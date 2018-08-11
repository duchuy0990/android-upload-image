package com.example.duchuynm.uploadimage;

import android.os.AsyncTask;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JDBCconnection extends AsyncTask<String, Void, Void> {
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://192.168.1.108:3306/test2"; //"jdbc:mysql://host:port/db_name"

    static final String USER = "root";
    static final String PASS = "";

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;

    public void insert(String name, Date time, String device) {
        try {
            String status = "chua xem";
            initJDBCconnection();
            String query = "INSERT INTO imageDetails(imageName,time,deviceSend,status)" +
                            "values(?,?,?,?)";
            preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1," "+name);
            preparedStatement.setDate(2, Date.valueOf(""+time));
            preparedStatement.setString(3," "+device);
            preparedStatement.setString(4,status);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        if(conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if(preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void initJDBCconnection() {
        try {
            Class.forName(JDBC_DRIVER).newInstance();
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(String... strings) {
        insert(strings[0],Date.valueOf(strings[1]),strings[2]);
        return null;
    }
}
