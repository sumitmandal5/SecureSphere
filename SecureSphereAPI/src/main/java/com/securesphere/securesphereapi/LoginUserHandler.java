package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securesphere.securesphereapi.constants.LoginUserHandlerConstants;
import com.securesphere.securesphereapi.service.CognitoUserService;
import com.securesphere.securesphereapi.utils.DecryptionUtil;
import com.securesphere.securesphereapi.utils.LogUtil;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;
    private final String userPoolId;

    public LoginUserHandler(CognitoUserService cognitoUserService,
                            String appClientId, String appClientSecret, String userPoolId) {
        this.cognitoUserService = cognitoUserService;
        this.appClientId = appClientId;
        this.appClientSecret = appClientSecret;
        this.userPoolId = userPoolId;
    }

    public LoginUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = DecryptionUtil.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = DecryptionUtil.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
        this.userPoolId = DecryptionUtil.decryptKey("MY_COGNITO_USERS_POOL_ID");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        LogUtil logUtil = new LogUtil(logger);
        String requestId = input.getRequestContext().getRequestId();
        String functionName = context.getFunctionName();
        String functionVersion = context.getFunctionVersion();
        logUtil.logInfo(requestId, "input received", "", functionName, functionVersion);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");//todo
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        try {
            String requestBodyJsonString = input.getBody();
            //logUtil.logInfo(requestId, "requestBody", requestBodyJsonString,
            //functionName, functionVersion);
            JsonObject requestBody = JsonParser.parseString(requestBodyJsonString).getAsJsonObject();
            String email = requestBody.get(LoginUserHandlerConstants.email.getValue()).getAsString();
            String password = requestBody.get(LoginUserHandlerConstants.password.getValue()).getAsString();
            JsonObject loginUserResult = cognitoUserService
                    .userLogin(email, password, appClientId, appClientSecret, userPoolId,
                            logUtil, requestId, functionName, functionVersion);
            logUtil.logInfo(requestId, "logged-in user successfully", "",
                    functionName, functionVersion);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(loginUserResult, JsonObject.class));
        } catch (UserNotConfirmedException ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("The email-id is not confirmed. " +
                    "Please confirm by clicking on the confirmation link sent to your registered email");
        } catch (PasswordResetRequiredException ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody(ex.awsErrorDetails().errorMessage());
        } catch (UserNotFoundException ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("User not found. Please register.");
        } catch (NotAuthorizedException ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("Incorrect User Name or Password.");
        } catch (AwsServiceException ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("Exception Occurred.");
        } catch (Exception ex) {
            logUtil.logError(requestId, "Exception",
                    ex.toString(), functionName,
                    functionVersion, 500);
            response.withBody("Exception Occurred.");
            response.setStatusCode(500);
        }

        return response;
    }
}

