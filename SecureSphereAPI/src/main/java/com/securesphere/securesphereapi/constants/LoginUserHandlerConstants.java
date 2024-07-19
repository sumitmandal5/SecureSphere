package com.securesphere.securesphereapi.constants;

public enum LoginUserHandlerConstants {
    email("email"),
    password("password"),
    idToken("idToken"),
    accessToken("accessToken"),
    //refreshToken("refreshToken"),
    expiresIn("expiresIn"),
    groups("groups"),
    name("name"),
    phone("phone");

    private final String strValue;

    private LoginUserHandlerConstants(String strValue) {
        this.strValue = strValue;
    }

    public String getValue() {
        return strValue;
    }
}
