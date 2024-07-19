package com.securesphere.securesphereapi.constants;

public enum CreateUserHandlerConstants {
    email("email"),
    password("password"),
    firstName("firstName"),
    lastName("lastName"),
    phone("phone");
    private final String strValue;

    private CreateUserHandlerConstants(String strValue) {
        this.strValue = strValue;
    }

    public String getValue() {
        return strValue;
    }
}
