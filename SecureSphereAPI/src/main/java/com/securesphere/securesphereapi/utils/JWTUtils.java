package com.securesphere.securesphereapi.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;

public class JWTUtils {
    public DecodedJWT deCodeJWT(String jwt, String region, String userPoolId, String audience) {
        RSAKeyProvider keyProvider = new AwsCognitoRSAKeyProvider(region, userPoolId);
        Algorithm algorithm = Algorithm.RSA256(keyProvider);
        //if we have to validate access, token "id" should be replaced with "access"
        JWTVerifier jwtVerifier = JWT.require(algorithm)
                .withAudience(audience)
                .withIssuer("https://cognito-idp." + region + ".amazonaws.com/" + userPoolId)
                .withClaim("token_use", "id")
                .build();
        DecodedJWT decoded = jwtVerifier.verify(jwt);
        return decoded;
    }

}
