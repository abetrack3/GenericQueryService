package com.abetrack3.GenericQueryService.Controller.QueryServiceCore;

import com.abetrack3.GenericQueryService.Controller.AppRuntimeConfiguration;
import com.abetrack3.GenericQueryService.Controller.Data.RequestHeaderNames;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtTokenValidator {

    private static final int BEARER_TOKEN_SUB_STRING_STARTING_INDEX = 7;
//    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
    private static final Map<String, RSAPublicKey> jwkToPublicKey = new HashMap<>();
    private static final List<String> allowedIssuers = AppRuntimeConfiguration.getTrustedIssuers();

    @NotNull
    private static String getTokenFromAuthorizationRequestHeader(HttpServletRequest request) {

        String token = request.getHeader(RequestHeaderNames.AUTHORIZATION);

        if (token == null) {
            return "";
        }

        return token.substring(BEARER_TOKEN_SUB_STRING_STARTING_INDEX);
    }

    @NotNull
    private static String getTokenFromCookies(HttpServletRequest request) throws URISyntaxException {

        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) {
            return "";
        }

        String originHost = getHostOfRequestOrigin(request);

        if (originHost.equals("")) {
            return "";
        }

        for (Cookie eachCookie : cookies) {
            if (eachCookie.getName().equals(originHost)) {
                return eachCookie.getValue();
            }
        }

        return "";
    }

    @NotNull
    private static String getHostOfRequestOrigin(HttpServletRequest request) throws URISyntaxException {

        String origin = request.getHeader(RequestHeaderNames.ORIGIN);

        if (origin == null) {
            origin = request.getHeader(RequestHeaderNames.REFERER);
        }

        if (origin == null) {
            return "";
        }

        return new URI(origin).getHost();
    }

    @NotNull
    public static String getToken(HttpServletRequest request) throws URISyntaxException {

        String token = getTokenFromAuthorizationRequestHeader(request);

        if (token.equals("")) {
            token = getTokenFromCookies(request);
        }

        return token;

    }

    private static String fetchJWKUrl(DecodedJWT token) {
        return token.getIssuer() + AppRuntimeConfiguration.getJwkHostSuffix();
    }

    private static RSAPublicKey loadPublicKey(DecodedJWT token) throws JwkException, MalformedURLException {

        String jwkUrlString = fetchJWKUrl(token);

        if (jwkToPublicKey.containsKey(jwkUrlString)) {
            return jwkToPublicKey.get(jwkUrlString);
        }

        JwkProvider provider = new UrlJwkProvider(new URL(jwkUrlString));

        RSAPublicKey publicKey = (RSAPublicKey) provider.get(token.getKeyId()).getPublicKey();
        jwkToPublicKey.put(jwkUrlString, publicKey);
        return publicKey;
    }

    public static DecodedJWT validate(String token) {
        try {

            final DecodedJWT jwt = JWT.decode(token);

            String tokenIssuerHostName = URI.create(jwt.getIssuer()).getHost();
            if (!allowedIssuers.contains(tokenIssuerHostName)) {
                throw new InvalidParameterException(String.format("Unknown Issuer %s", jwt.getIssuer()));
            }

            RSAPublicKey publicKey = loadPublicKey(jwt);

            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(jwt.getIssuer())
                    .build();

            verifier.verify(token);
            return jwt;

        } catch (Exception e) {
//            logger.error("Failed to validate JWT", e);
            throw new InvalidParameterException("JWT validation failed: " + e.getMessage());
        }
    }

}
