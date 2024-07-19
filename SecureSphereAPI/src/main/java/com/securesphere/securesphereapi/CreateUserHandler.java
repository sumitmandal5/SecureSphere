package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securesphere.securesphereapi.service.CognitoUserService;
import com.securesphere.securesphereapi.utils.DecryptionUtil;
import com.securesphere.securesphereapi.utils.LogUtil;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AliasExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import java.util.HashMap;
import java.util.Map;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public CreateUserHandler(CognitoUserService cognitoUserService,
                             String appClientId,String appClientSecret){
        this.cognitoUserService=cognitoUserService;
        this.appClientId=appClientId;
        this.appClientSecret=appClientSecret;
    }

    public CreateUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = DecryptionUtil.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = DecryptionUtil.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        LogUtil logUtil = new LogUtil(logger);
        String requestId = input.getRequestContext().getRequestId();
        String functionName = context.getFunctionName();
        String functionVersion = context.getFunctionVersion();
        logUtil.logInfo(requestId, "input received", "",
                functionName, functionVersion);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");//todo this header to be set
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String requestBody = input.getBody();
            //logUtil.logInfo(requestId, "requestBody", requestBody,functionName, functionVersion);
            JsonObject userDetails = JsonParser.parseString(requestBody).getAsJsonObject();
            JsonObject createUserResult = cognitoUserService.createUser(userDetails, appClientId,
                    appClientSecret, logUtil, requestId,functionName,functionVersion );
            logUtil.logInfo(requestId, "created user successfully", "",
                    functionName, functionVersion);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(createUserResult, JsonObject.class));
        } catch (AliasExistsException | InvalidPasswordException | UsernameExistsException ex) {
            logUtil.logError(input.getRequestContext().getRequestId(), "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody(ex.awsErrorDetails().errorMessage());
        } catch (AwsServiceException ex) {
            logUtil.logError(input.getRequestContext().getRequestId(), "Exception",
                    ex.toString(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("Exception Occurred");
        } catch (Exception ex) {
            logUtil.logError(input.getRequestContext().getRequestId(), "Exception",
                    ex.toString(), functionName,
                    functionVersion, 500);
            response.withBody("Exception Occurred");
            response.setStatusCode(500);
        }
        return response;
    }

}
