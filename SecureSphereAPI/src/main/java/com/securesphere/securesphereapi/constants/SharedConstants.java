package com.securesphere.securesphereapi.constants;

public enum SharedConstants {
    isSuccessful("isSuccessful"),
    statusCode("statusCode"),
    isConfirmed("isConfirmed");


    private final String strValue;

    private SharedConstants(String strValue) {
        this.strValue = strValue;
    }

    public String getValue() {
        return strValue;
    }
}
