package com.securesphere.securesphereapi.service;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.securesphere.securesphereapi.constants.CreateUserHandlerConstants;
import com.securesphere.securesphereapi.constants.LoginUserHandlerConstants;
import com.securesphere.securesphereapi.constants.SharedConstants;
import com.securesphere.securesphereapi.utils.JWTUtils;
import com.securesphere.securesphereapi.utils.LogUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CognitoUserServiceTest {

    @Mock
    CognitoIdentityProviderClient cognitoIdentityProviderClientMock;
    @Mock
    LogUtil logUtilMock;

    @Mock
    SignUpResponse signUpResponseMock;
    @Mock
    SdkHttpResponse sdkHttpResponseMock;
    @Mock
    InitiateAuthResponse initiateAuthResponseMock;
    @Mock
    AuthenticationResultType authenticationResultTypeMock;
    @Mock
    JWTUtils jwtUtilsMock;
    @Mock
    DecodedJWT decodedJWTMock;
    @Mock
    Claim claimMock;
    @InjectMocks
    CognitoUserService cognitoUserserviceMock;

    private static JsonObject userDetails;

    private static String appClientId;
    private static String appClientSecret;

    private static String requestId;
    private static String functionName;
    private static String functionVersion;
    private static String email;
    private static String password;
    private static String userPoolId;

    @BeforeAll
    private static void initializeObjects() {
        userDetails = new JsonObject();
        userDetails.addProperty(CreateUserHandlerConstants.email.getValue(), "test@email.com");
        userDetails.addProperty(CreateUserHandlerConstants.password.getValue(), "12345678");
        userDetails.addProperty(CreateUserHandlerConstants.firstName.getValue(), "testFirstname");
        userDetails.addProperty(CreateUserHandlerConstants.lastName.getValue(), "testLastName");
        userDetails.addProperty(CreateUserHandlerConstants.phone.getValue(), "+448888888888");

        appClientId = "testAppClientId";
        appClientSecret = "testAppClientSecret";
        requestId = "testRequestId";
        functionName = "testFunctionName";
        functionVersion = "testFunctionVersion";

        email = "test@email.com";
        password = "testPassword";
        userPoolId = "testUserPoolId";
    }

    @Test
    public void testCreateUser_WhenUserDetailsProvided_ReturnSuccessAfterUserCreation() {
        doNothing().when(logUtilMock).logInfo(any(), any(), any(), any(), any());

        when(cognitoIdentityProviderClientMock.signUp(any(SignUpRequest.class))).thenReturn(signUpResponseMock);
        when(signUpResponseMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.isSuccessful()).thenReturn(true);
        when(sdkHttpResponseMock.statusCode()).thenReturn(200);
        when(signUpResponseMock.userConfirmed()).thenReturn(false);
        JsonObject createUserResult = cognitoUserserviceMock.createUser(userDetails, appClientId,
                appClientSecret, logUtilMock, requestId, functionName, functionVersion);
        assertTrue(createUserResult.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "Successful HTTP response isSuccessful true");
        assertEquals(200, createUserResult.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "Successful HTTP response should have status code 200");
        assertFalse(createUserResult.get(SharedConstants.isConfirmed.getValue()).getAsBoolean(),
                "isConfirmed should be false as at the time of registration user is not confirmed");

    }

    @Test
    public void testCreateUser_WhenUserNotCreated_ReturnUnsuccessfulResult() {
        doNothing().when(logUtilMock).logInfo(any(), any(), any(), any(), any());

        when(cognitoIdentityProviderClientMock.signUp(any(SignUpRequest.class))).thenReturn(signUpResponseMock);
        when(signUpResponseMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.isSuccessful()).thenReturn(false);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);
        when(signUpResponseMock.userConfirmed()).thenReturn(false);
        JsonObject createUserResult = cognitoUserserviceMock.createUser(userDetails, appClientId,
                appClientSecret, logUtilMock, requestId, functionName, functionVersion);
        assertFalse(createUserResult.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "HTTP response isSuccessful false");
        assertEquals(500, createUserResult.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "HTTP response should have status code 500");
        assertFalse(createUserResult.get(SharedConstants.isConfirmed.getValue()).getAsBoolean(),
                "isConfirmed should be false as at the time of registration user is not confirmed");
    }

    @Test
    public void testUserLogin_WhenSuccessful_ReturnSuccessfulResult() {

        CognitoUserService MockCognitoUserService = spy(new CognitoUserService(
                cognitoIdentityProviderClientMock));
        when(MockCognitoUserService.getJWTUtils()).thenReturn(jwtUtilsMock);

        doNothing().when(logUtilMock).logInfo(any(), any(), any(), any(), any());
        when(cognitoIdentityProviderClientMock.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(initiateAuthResponseMock);
        when(initiateAuthResponseMock.authenticationResult()).thenReturn(authenticationResultTypeMock);
        when(initiateAuthResponseMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.isSuccessful()).thenReturn(true);
        when(sdkHttpResponseMock.statusCode()).thenReturn(200);

        when(authenticationResultTypeMock.idToken()).thenReturn("testIdToken");
        when(authenticationResultTypeMock.accessToken()).thenReturn("testAccessToken");
        //when(authenticationResultTypeMock.refreshToken()).thenReturn("testRefreshToken");
        when(authenticationResultTypeMock.expiresIn()).thenReturn(100);

        when(jwtUtilsMock.deCodeJWT(any(), any(), any(), any())).thenReturn(decodedJWTMock);
        when(decodedJWTMock.getClaim("cognito:groups")).thenReturn(claimMock);
        String[] claimArray = new String[1];
        claimArray[0] = "testGroup";
        when(claimMock.asArray(String.class)).thenReturn(claimArray);
        when(decodedJWTMock.getClaim("name")).thenReturn(claimMock);
        when(decodedJWTMock.getClaim("name").asString()).thenReturn("testName");
        when(decodedJWTMock.getClaim("phone_number")).thenReturn(claimMock);
        when(decodedJWTMock.getClaim("phone_number").asString()).thenReturn("+448888888888");

        JsonObject loginUserResult = MockCognitoUserService.userLogin(email, password,
                appClientId, appClientSecret, userPoolId, logUtilMock, requestId, functionName, functionVersion);
        assertTrue(loginUserResult.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "In case of successful response, HTTP response isSuccessful should be true");
        assertEquals(200, loginUserResult.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "successful HTTP response should have status code 200");
        assertEquals(loginUserResult.get(LoginUserHandlerConstants.idToken.getValue())
                .getAsString(), "testIdToken");
        assertEquals(loginUserResult.get(LoginUserHandlerConstants.accessToken.getValue())
                .getAsString(), "testAccessToken");
        //assertEquals(loginUserResult.get(LoginUserHandlerConstants.refreshToken.getValue())
        //.getAsString(), "testRefreshToken");
        assertEquals(loginUserResult.get(LoginUserHandlerConstants.phone.getValue())
                .getAsString(), "+448888888888");

    }

    @Test
    public void testUserLogin_WhenUnSuccessful_ReturnUnSuccessfulResult() {
        doNothing().when(logUtilMock).logInfo(any(), any(), any(), any(), any());

        when(cognitoIdentityProviderClientMock.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(initiateAuthResponseMock);
        when(initiateAuthResponseMock.authenticationResult()).thenReturn(authenticationResultTypeMock);
        when(initiateAuthResponseMock.sdkHttpResponse()).thenReturn(sdkHttpResponseMock);
        when(sdkHttpResponseMock.isSuccessful()).thenReturn(false);
        when(sdkHttpResponseMock.statusCode()).thenReturn(500);

        String[] claimArray = new String[1];
        claimArray[0] = "testGroup";

        JsonObject loginUserResult = cognitoUserserviceMock.userLogin(email, password, appClientId, appClientSecret,
                userPoolId, logUtilMock, requestId, functionName, functionVersion);
        assertFalse(loginUserResult.get(SharedConstants.isSuccessful.getValue()).getAsBoolean(),
                "HTTP response isSuccessful false");
        assertEquals(500, loginUserResult.get(SharedConstants.statusCode.getValue()).getAsInt(),
                "HTTP response should have status code 500");
    }
}
