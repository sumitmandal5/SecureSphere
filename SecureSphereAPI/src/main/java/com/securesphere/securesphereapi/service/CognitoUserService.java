package com.securesphere.securesphereapi.service;


import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.securesphere.securesphereapi.constants.CreateUserHandlerConstants;
import com.securesphere.securesphereapi.constants.LoginUserHandlerConstants;
import com.securesphere.securesphereapi.constants.SharedConstants;
import com.securesphere.securesphereapi.utils.JWTUtils;
import com.securesphere.securesphereapi.utils.LogUtil;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*https://docs.aws.amazon.com/cognito/latest/developerguide/service_code_examples.html*/
//For Authorization try Amazon Verified Permissions
public class CognitoUserService {
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private String region;

    public CognitoUserService(CognitoIdentityProviderClient cognitoIdentityProviderClient) {
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    }

    public CognitoUserService(String region) {
        this.region = region;
        this.cognitoIdentityProviderClient = CognitoIdentityProviderClient
                .builder().region(Region.of(region)).build();
    }

    public JsonObject createUser(JsonObject user, String appClientId,
                                 String appClientSecret, LogUtil logUtil, String requestId,
                                 String functionName, String functionVersion) {
        logUtil.logInfo(requestId, "createUser service invoked", "",
                functionName, functionVersion);
        String email = user.get(CreateUserHandlerConstants.email.getValue()).getAsString();
        String password = user.get(CreateUserHandlerConstants.password.getValue()).getAsString();
        String firstName = user.get(CreateUserHandlerConstants.firstName.getValue()).getAsString();
        String lastName = user.get(CreateUserHandlerConstants.lastName.getValue()).getAsString();
        String phoneNmuber = user.get(CreateUserHandlerConstants.phone.getValue()).getAsString();
        String UserId = UUID.randomUUID().toString();

        AttributeType emailAttribute = AttributeType.builder()
                .name(CreateUserHandlerConstants.email.getValue())
                .value(email)
                .build();

        AttributeType nameAttribute = AttributeType.builder()
                .name("name")
                .value(firstName + " " + lastName)
                .build();

        AttributeType phoneNumberAttribute = AttributeType.builder()
                .name("phone_number")
                .value(phoneNmuber)
                .build();

        AttributeType userIdAttribute = AttributeType.builder()
                .name("custom:userId")
                .value(UserId)
                .build();

        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(emailAttribute);
        attributes.add(nameAttribute);
        attributes.add(userIdAttribute);
        attributes.add(phoneNumberAttribute);

        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);

        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username(email)
                .password(password)
                .userAttributes(attributes)
                .clientId(appClientId)
                .secretHash(generatedSecretHash)
                .build();
        SignUpResponse signUpResponse = cognitoIdentityProviderClient.signUp(signUpRequest);
        boolean isSuccessful = signUpResponse.sdkHttpResponse().isSuccessful();
        int statusCode = signUpResponse.sdkHttpResponse().statusCode();
        boolean userConfirmed = signUpResponse.userConfirmed();
        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(SharedConstants.isSuccessful.getValue(), isSuccessful);
        createUserResult.addProperty(SharedConstants.statusCode.getValue(), statusCode);
        //createUserResult.addProperty("cognitoUserId", signUpResponse.userSub());
        createUserResult.addProperty(SharedConstants.isConfirmed.getValue(), userConfirmed);
        logUtil.logInfo(requestId, SharedConstants.isSuccessful.getValue(), String.valueOf(isSuccessful),
                functionName, functionVersion);
        logUtil.logInfo(requestId, SharedConstants.statusCode.getValue(), String.valueOf(statusCode),
                functionName, functionVersion);
        logUtil.logInfo(requestId, SharedConstants.isConfirmed.getValue(), String.valueOf(userConfirmed),
                functionName, functionVersion);
        return createUserResult;
    }

    public JsonObject confirmUserSignup(String appClientId, String appClientSecret, String email, String confirmationCode,
                                        LogUtil logUtil, String requestId,
                                        String functionName, String functionVersion) {
        logUtil.logInfo(requestId, "confirmUserSignup service invoked", "",
                functionName, functionVersion);
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .clientId(appClientId)
                .secretHash(generatedSecretHash)
                .username(email)
                .confirmationCode(confirmationCode)
                .build();
        ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProviderClient.confirmSignUp(confirmSignUpRequest);
        boolean isSuccessful = confirmSignUpResponse.sdkHttpResponse().isSuccessful();
        int statusCode = confirmSignUpResponse.sdkHttpResponse().statusCode();
        JsonObject confirmUserResponse = new JsonObject();
        confirmUserResponse.addProperty(SharedConstants.isSuccessful.getValue(), isSuccessful);
        confirmUserResponse.addProperty(SharedConstants.statusCode.getValue(), statusCode);
        logUtil.logInfo(requestId, SharedConstants.isSuccessful.getValue(), String.valueOf(isSuccessful),
                functionName, functionVersion);
        logUtil.logInfo(requestId, SharedConstants.statusCode.getValue(), String.valueOf(statusCode),
                functionName, functionVersion);
        return confirmUserResponse;

    }

    public JsonObject userLogin(String email, String password, String appClientId, String appClientSecret,
                                String userPoolId, LogUtil logUtil, String requestId,
                                String functionName, String functionVersion) {
        logUtil.logInfo(requestId, "userLogin service invoked", "",
                functionName, functionVersion);
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", email);
        authParams.put("PASSWORD", password);
        authParams.put("SECRET_HASH", generatedSecretHash);

        InitiateAuthRequest initiateAuthRequest = InitiateAuthRequest.builder()
                .clientId(appClientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParams)
                .build();
        InitiateAuthResponse initiateAuthResponse = cognitoIdentityProviderClient.initiateAuth(initiateAuthRequest);
        AuthenticationResultType authenticationResultType = initiateAuthResponse.authenticationResult();
        JsonObject loginUserResult = new JsonObject();
        boolean isSuccessful = initiateAuthResponse.sdkHttpResponse().isSuccessful();
        int statusCode = initiateAuthResponse.sdkHttpResponse().statusCode();
        loginUserResult.addProperty(SharedConstants.isSuccessful.getValue(), isSuccessful);
        loginUserResult.addProperty(SharedConstants.statusCode.getValue(), statusCode);

        if (initiateAuthResponse.sdkHttpResponse().isSuccessful()) {
            loginUserResult.addProperty(LoginUserHandlerConstants.idToken.getValue(), authenticationResultType.idToken());
            loginUserResult.addProperty(LoginUserHandlerConstants.accessToken.getValue(), authenticationResultType.accessToken());
            //loginUserResult.addProperty(LoginUserHandlerConstants.refreshToken.getValue(), authenticationResultType.refreshToken());
            loginUserResult.addProperty(LoginUserHandlerConstants.expiresIn.getValue(), authenticationResultType.expiresIn());
            loginUserResult.addProperty(LoginUserHandlerConstants.email.getValue(), email);
            JWTUtils jwtUtils = getJWTUtils();
            DecodedJWT decodedJWT = jwtUtils.deCodeJWT(authenticationResultType.idToken(),
                    region, userPoolId, appClientId);
            String[] groups = decodedJWT.getClaim("cognito:groups").asArray(String.class);
            loginUserResult.addProperty(LoginUserHandlerConstants.groups.getValue(), Arrays.toString(groups));
            String name = decodedJWT.getClaim("name").asString();
            loginUserResult.addProperty(LoginUserHandlerConstants.name.getValue(), name);
            String phone = decodedJWT.getClaim("phone_number").asString();
            loginUserResult.addProperty(LoginUserHandlerConstants.phone.getValue(), phone);

        }
        logUtil.logInfo(requestId, SharedConstants.isSuccessful.getValue(), String.valueOf(isSuccessful),
                functionName, functionVersion);
        logUtil.logInfo(requestId, SharedConstants.statusCode.getValue(), String.valueOf(statusCode),
                functionName, functionVersion);
        return loginUserResult;
    }

    protected JWTUtils getJWTUtils() {
        return new JWTUtils();
    }

    public JsonObject getUser(String accessToken, LogUtil logUtil, String requestId,
                              String functionName, String functionVersion) {
        logUtil.logInfo(requestId, "getUser service invoked", "",
                functionName, functionVersion);
        GetUserRequest getUserRequest = GetUserRequest.builder().accessToken(accessToken).build();
        GetUserResponse getUserResponse = cognitoIdentityProviderClient.getUser(getUserRequest);
        boolean isSuccessful = getUserResponse.sdkHttpResponse().isSuccessful();
        int statusCode = getUserResponse.sdkHttpResponse().statusCode();
        JsonObject getuserResult = new JsonObject();
        getuserResult.addProperty(SharedConstants.isSuccessful.getValue(), isSuccessful);
        getuserResult.addProperty(SharedConstants.statusCode.getValue(), statusCode);

        List<AttributeType> userAttributes = getUserResponse.userAttributes();
        JsonObject userDetails = new JsonObject();
        userAttributes.stream().forEach((attribute) -> {
            userDetails.addProperty(attribute.name(), attribute.value());
        });
        getuserResult.add("user", userDetails);
        logUtil.logInfo(requestId, SharedConstants.isSuccessful.getValue(), String.valueOf(isSuccessful),
                functionName, functionVersion);
        logUtil.logInfo(requestId, SharedConstants.statusCode.getValue(), String.valueOf(statusCode),
                functionName, functionVersion);
        return getuserResult;
    }

    public String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating secret hash.");
        }
    }
}
