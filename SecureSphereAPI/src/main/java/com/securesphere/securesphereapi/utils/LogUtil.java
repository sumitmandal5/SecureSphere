package com.securesphere.securesphereapi.utils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LogUtil {

    private final LambdaLogger lambdaLogger;

    public LogUtil(LambdaLogger lambdaLogger) {
        this.lambdaLogger = lambdaLogger;
    }

    public String dateFormatter(long epoch) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(epoch);
        String formattedDate = dateFormatter.format(date);
        if (!formattedDate.contains("UTC")) {
            formattedDate += " UTC";
        }
        return formattedDate;
    }

    public void logInfo(String requestId, String message, String details,String functionName,
                        String functionVersion) {
        String logEntry = String.format(
                "{\"timestamp\": \"%s\", \"log_level\": \"INFO\", \"request_id\": \"%s\", " +
                        "\"message\": \"%s\", \"details\": \"%s\", \"function_name\": \"%s\"," +
                        " \"function_version\": \"%s\"}",
                dateFormatter(System.currentTimeMillis()), requestId, message, details,
                functionName, functionVersion);
        lambdaLogger.log(logEntry);
    }

    public void logError(String requestId, String message, String details,String functionName,
                         String functionVersion, int statusCode) {
        String logEntry = String.format(
                "{\"timestamp\": \"%s\", \"log_level\": \"Error\", \"request_id\": \"%s\", " +
                        "\"message\": \"%s\", \"details\": \"%s\", \"function_name\": \"%s\", " +
                        "\"function_version\": \"%s\",\"status_code\": \"%s\"}",
                dateFormatter(System.currentTimeMillis()), requestId, message,details,
                functionName, functionVersion, statusCode);
        lambdaLogger.log(logEntry);
    }
}
