package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securesphere.securesphereapi.constants.CreateUserHandlerConstants;
import com.securesphere.securesphereapi.constants.LoginUserHandlerConstants;
import com.securesphere.securesphereapi.constants.SharedConstants;
import com.securesphere.securesphereapi.service.CognitoUserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginUserHandlerTest {

    @Mock
    static CognitoUserService cognitoUserServiceMock;
    @Mock
    APIGatewayProxyRequestEvent event;
    @Mock
    Context context;
    @Mock
    LambdaLogger lambdaLoggerMock;
    @Mock
    APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext;
    @Mock
    UserNotConfirmedException UserNotConfirmedExceptionMock;
    @Mock
    AwsErrorDetails awsErrorDetailsMock;
    @Mock
    SdkHttpResponse sdkHttpResponseMock;
    @Mock
    PasswordResetRequiredException PasswordResetRequiredExceptionMock;
    @Mock
    UserNotFoundException UserNotFoundExceptionMock;
    @Mock
    NotAuthorizedException NotAuthorizedExceptionMock;
    @Mock
    AwsServiceException AwsServiceExceptionMock;
    @Mock
    RuntimeException RuntimeExceptionMock;
    @InjectMocks
    LoginUserHandler handler;

    private static JsonObject loginDetails;

    @BeforeAll
    private static void initializeLoginDetails() {
        loginDetails = new JsonObject();
        loginDetails.addProperty(CreateUserHandlerConstants.email.getValue(), "test@email.com");
        loginDetails.addProperty(CreateUserHandlerConstants.password.getValue(), "12345678");
    }

    @Test
    public void testHandleRequest_WhenValidDetailsProvided_ReturnSuccessfulResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);


        JsonObject loginUserResult = new JsonObject();
        loginUserResult.addProperty(SharedConstants.isSuccessful.getValue(), true);
        loginUserResult.addProperty(SharedConstants.statusCode.getValue(), "200");
        loginUserResult.addProperty(LoginUserHandlerConstants.idToken.getValue(), "idToken");
        loginUserResult.addProperty(LoginUserHandlerConstants.accessToken.getValue(), "accessToken");
        //loginUserResult.addProperty(LoginUserHandlerConstants.refreshToken.getValue(), "refreshToken");
        loginUserResult.addProperty(LoginUserHandlerConstants.expiresIn.getValue(), "100");
        loginUserResult.addProperty(LoginUserHandlerConstants.email.getValue(), "test@email.com");
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenReturn(loginUserResult);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        assertTrue(responseBodyJson.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "Successful HTTP response isSuccessful true");
        assertEquals(200, responseBodyJson.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "Successful HTTP response should have status code 200");
        assertEquals("idToken", responseBodyJson.get(LoginUserHandlerConstants.idToken.getValue())
                        .getAsString(), "Successful HTTP response should have status code 200");
        assertEquals("accessToken", responseBodyJson.get(LoginUserHandlerConstants.accessToken.getValue())
                        .getAsString(), "Successful HTTP response should have status code 200");
        /*assertEquals("refreshToken", responseBodyJson.get(LoginUserHandlerConstants.refreshToken.getValue())
                .getAsString(), "Successful HTTP response should have status code 200");*/
        assertEquals(100, responseBodyJson.get(LoginUserHandlerConstants.expiresIn.getValue()).getAsInt(),
                "Successful HTTP response should have status code 200");
        assertEquals("test@email.com", responseBodyJson.get(LoginUserHandlerConstants.email.getValue())
                .getAsString(),"Successful HTTP response should have status code 200");

        verify(lambdaLoggerMock, times(2)).log(anyString());

    }

    @Test
    public void testHandleRequest_WhenUserNotConfirmed_ReturnUserNotConfirmedResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(UserNotConfirmedExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(UserNotConfirmedExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("The email-id is not confirmed. Please confirm by clicking on " +
                        "the confirmation link sent to your registered email", responseEvent.getBody(),
                "HandleRequest function should return email-id is not confirmed " +
                        "if UserNotConfirmedException is thrown.");
    }

    @Test
    public void testHandleRequest_WhenPasswordResetRequired_ReturnPasswordResetRequiredResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(PasswordResetRequiredExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(PasswordResetRequiredExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
    }

    @Test
    public void testHandleRequest_WhenUserNotFound_ReturnUserNotFoundResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(UserNotFoundExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(UserNotFoundExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("User not found. Please register.", responseEvent.getBody(),
                "HandleRequest function should return email-id is not confirmed " +
                        "if UserNotFoundException is thrown.");
    }

    @Test
    public void testHandleRequest_WhenIncorrectUserNameOrPassword_ReturnIncorrectUserNameOrPasswordResponse() {

        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(NotAuthorizedExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(NotAuthorizedExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("Incorrect User Name or Password.", responseEvent.getBody(),
                "HandleRequest function should return Incorrect User Name or Password " +
                        "if NotAuthorizedException is thrown.");

    }

    @Test
    public void testHandleRequest_WhenAwsServiceException_ReturnExceptionOccurredResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(AwsServiceExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(AwsServiceExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("Exception Occurred.", responseEvent.getBody(),
                "HandleRequest function should return Exception Occurred " +
                        "if AwsServiceException is thrown.");
    }

    @Test
    public void testHandleRequest_WhenException_ReturnExceptionOccurredResponse() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        String loginDetailsJsonString = new Gson().toJson(loginDetails, JsonObject.class);
        when(event.getBody()).thenReturn(loginDetailsJsonString);

        when(cognitoUserServiceMock.userLogin(any(), any(), any(), any(), any(), any(), any(),any(),any()))
                .thenThrow(RuntimeExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("Exception Occurred.", responseEvent.getBody(),
                "HandleRequest function should return Exception Occurred " +
                        "if ant other Exception is thrown.");
    }

}
