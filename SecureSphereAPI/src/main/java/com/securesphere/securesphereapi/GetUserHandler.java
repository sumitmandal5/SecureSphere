package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.securesphere.securesphereapi.service.CognitoUserService;
import com.securesphere.securesphereapi.utils.LogUtil;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;
/*
This functionailty is not needed now. It has been commented out from template.yaml.
If required in future, it has to be uncommented from the template.yaml and after deployment
the lambda function would need to be added to the KMS.
 */
public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CognitoUserService cognitoUserService;

    public GetUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
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
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            Map<String, String> requestHeaders = input.getHeaders();
            String requestBody = input.getBody();
            //logUtil.logInfo(requestId, "requestBody", requestBody,functionName, functionVersion);
            JsonObject userDetails = cognitoUserService.getUser(requestHeaders.get("AccessToken"),
                    logUtil, requestId, functionName, functionVersion);
            response.withBody(new Gson().toJson(userDetails, JsonObject.class));
            response.setStatusCode(200);
        } catch (UserNotFoundException ex) {
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
