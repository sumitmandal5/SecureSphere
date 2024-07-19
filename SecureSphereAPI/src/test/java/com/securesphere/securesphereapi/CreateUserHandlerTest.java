package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securesphere.securesphereapi.constants.CreateUserHandlerConstants;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.http.SdkHttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {

    @Mock
    CognitoUserService cognitoUserService;
    @Mock
    APIGatewayProxyRequestEvent event;
    @Mock
    Context context;
    @Mock
    LambdaLogger lambdaLoggerMock;
    @Mock
    APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext;
    @Mock
    UsernameExistsException usernameExistsExceptionMock;
    @Mock
    AwsErrorDetails awsErrorDetailsMock;
    @Mock
    SdkHttpResponse sdkHttpResponseMock;
    @Mock
    InvalidPasswordException invalidPasswordExceptionMock;
    @Mock
    AwsServiceException awsServiceExceptionMock;
    @Mock
    RuntimeException exceptionMock;
    @InjectMocks
    CreateUserHandler handler;

    private static JsonObject userDetails;

    @BeforeAll
    private static void initializeUserDetails() {
        userDetails = new JsonObject();
        userDetails.addProperty(CreateUserHandlerConstants.email.getValue(), "test@email.com");
        userDetails.addProperty(CreateUserHandlerConstants.password.getValue(), "12345678");
        userDetails.addProperty(CreateUserHandlerConstants.firstName.getValue(), "testFirstname");
        userDetails.addProperty(CreateUserHandlerConstants.lastName.getValue(), "testLastName");
        userDetails.addProperty(CreateUserHandlerConstants.phone.getValue(), "+448888888888");
    }

    @Test
    public void testHandleRequest_WhenValidDetailsProvided_ReturnSuccessfulResponse() {

        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(event.getBody()).thenReturn(userDetailsJsonString);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        when(context.getLogger()).thenReturn(lambdaLoggerMock);

        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(SharedConstants.isSuccessful.getValue(), true);
        createUserResult.addProperty(SharedConstants.statusCode.getValue(), "200");
        createUserResult.addProperty(SharedConstants.isConfirmed.getValue(), "false");

        when(cognitoUserService.createUser(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(createUserResult);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        assertTrue(responseBodyJson.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "Successful HTTP response isSuccessful true");
        assertEquals(200, responseBodyJson.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "Successful HTTP response should have status code 200");
        assertFalse(responseBodyJson.get(SharedConstants.isConfirmed.getValue()).getAsBoolean(),
                "isConfirmed should be false as at the time of registration user is not confirmed");

        verify(lambdaLoggerMock, times(2)).log(anyString());
    }

    @Test
    public void testHandleRequest_WhenExistingUserNameProvided_ReturnUnsuccessfulResponse() {
        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(event.getBody()).thenReturn(userDetailsJsonString);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        when(context.getLogger()).thenReturn(lambdaLoggerMock);

        when(usernameExistsExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.errorMessage()).thenReturn("username exists");
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserService.createUser(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(usernameExistsExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("username exists", responseEvent.getBody(),
                "HandleRequest function should return username exists " +
                        "if usernameExistsException is thrown.");

    }

    @Test
    public void testHandleRequest_WhenInvalidPasswordProvided_ReturnUnsuccessfulResponse() {
        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(event.getBody()).thenReturn(userDetailsJsonString);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        when(context.getLogger()).thenReturn(lambdaLoggerMock);

        when(invalidPasswordExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.errorMessage()).thenReturn("invalid password");
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserService.createUser(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(invalidPasswordExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("invalid password", responseEvent.getBody(),
                "HandleRequest function should return invalid password " +
                        "if usernameExistsException is thrown.");
    }

    @Test
    public void testHandleRequest_WhenAwsServiceException_ReturnUnsuccessfulResponse() {
        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(event.getBody()).thenReturn(userDetailsJsonString);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        when(context.getLogger()).thenReturn(lambdaLoggerMock);

        when(awsServiceExceptionMock.awsErrorDetails()).thenReturn(awsErrorDetailsMock);
        when(awsErrorDetailsMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(cognitoUserService.createUser(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(awsServiceExceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("Exception Occurred", responseEvent.getBody(),
                "HandleRequest function should return Exception Occurred " +
                        "if AwsServiceException is thrown.");
    }

    @Test
    public void testHandleRequest_WhenException_ReturnUnsuccessfulResponse() {
        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(event.getBody()).thenReturn(userDetailsJsonString);
        when(event.getRequestContext()).thenReturn(proxyRequestContext);
        when(event.getRequestContext().getRequestId()).thenReturn("testRequestId");

        when(context.getLogger()).thenReturn(lambdaLoggerMock);

        when(cognitoUserService.createUser(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(exceptionMock);

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(event, context);

        assertEquals(500, responseEvent.getStatusCode(),
                "UnSuccessful HTTP response should have status code 500");
        assertEquals("Exception Occurred", responseEvent.getBody(),
                "HandleRequest function should return Exception Occurred " +
                        "if Exception is thrown.");
    }


}
