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
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;

import java.util.HashMap;
import java.util.Map;

/*
This functionailty is not needed now as user confirmation is done using the link sent from Cognito.
It has been commented out from template.yaml.
If required in future, it has to be uncommented from the template.yaml and after deployment
the lambda function would need to be added to the KMS.
 */
public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public ConfirmUserHandler() {
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
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {
            String requestBodyJsonString = input.getBody();
            JsonObject requestBody = JsonParser.parseString(requestBodyJsonString).getAsJsonObject();
            //logUtil.logInfo(requestId, "requestBody", requestBodyJsonString,functionName, functionVersion);
            String email = requestBody.get("email").getAsString();
            String confirmationCode = requestBody.get("code").getAsString();
            String trimmedappClientId = appClientId.trim();
            String trimmedappClientSecret = appClientSecret.trim();
            JsonObject confirmUserResult = cognitoUserService
                    .confirmUserSignup(trimmedappClientId, trimmedappClientSecret,
                            email, confirmationCode, logUtil, requestId, functionName, functionVersion);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(confirmUserResult, JsonObject.class));
        } catch (InvalidParameterException | ExpiredCodeException ex) {
            logUtil.logError(requestId, ex.getCause().toString(),
                    ex.awsErrorDetails().errorMessage(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody(ex.awsErrorDetails().errorMessage());
        } catch (AwsServiceException ex) {
            logUtil.logError(requestId, ex.getCause().toString(),
                    ex.awsErrorDetails().errorMessage(), functionName,
                    functionVersion, ex.awsErrorDetails().sdkHttpResponse().statusCode());
            response.withStatusCode(500);
            response.withBody("Exception Occurred");
        } catch (Exception ex) {
            logUtil.logError(requestId, "Exception",
                    ex.getMessage(), functionName,
                    functionVersion, 500);
            response.withBody("Exception Occurred");
            response.setStatusCode(500);
        }
        return response;
    }

}
