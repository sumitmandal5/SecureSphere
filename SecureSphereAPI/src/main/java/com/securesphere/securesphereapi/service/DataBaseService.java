package com.securesphere.securesphereapi.service;

import com.securesphere.securesphereapi.utils.LogUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;


public class DataBaseService {

    private final String DBURL;

    public DataBaseService(String dbURL) {
        this.DBURL = dbURL;
    }

    /*https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-lambda-tutorial.html*/
    public void insertUserData(String name, String email, String phone,
                               String userUUID, String dbUserName, String dbPassword,
                               LogUtil logUtil, String requestId,
                               String functionName, String functionVersion) {
        logUtil.logInfo(requestId, "Inserting User Data into the DB", "",
                functionName, functionVersion);
        try {
            //https://dev.mysql.com/doc/connector-j/8.1/en/connector-j-usagenotes-connect-drivermanager.html
            Connection conn = DriverManager.getConnection(this.DBURL, dbUserName, dbPassword);
            long currentTimeMillis = System.currentTimeMillis();

            // Convert the milliseconds to a java.sql.Timestamp in UTC
            Timestamp utcTimestamp = new Timestamp(currentTimeMillis);

            String insertQuery = "INSERT INTO user(full_name,email,phone,user_uuid,create_date) VALUES (?, ?, ?, ?,?)";
            PreparedStatement statement = conn.prepareStatement(insertQuery);
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, userUUID);
            statement.setTimestamp(5, utcTimestamp);
            statement.executeUpdate();

        } catch (SQLException ex) {
            logUtil.logError(requestId, "Exception occurred while inserting data to DB",
                    ex.getMessage(), functionName, functionVersion, 500);
        } catch (Exception ex) {
            logUtil.logError(requestId, "Exception",
                    ex.getMessage(), functionName,
                    functionVersion, 500);
        }

    }
}
