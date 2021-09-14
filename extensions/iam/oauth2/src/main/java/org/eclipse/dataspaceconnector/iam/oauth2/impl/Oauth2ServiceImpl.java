/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.eclipse.dataspaceconnector.iam.oauth2.impl.Fingerprint.sha1Base64Fingerprint;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {
    private static final long EXPIRATION = 300; // 5 minutes

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Oauth2Configuration configuration;

    private final RSAKeyProvider credentialProvider;
    private final JWTVerifier verifier;

    public Oauth2ServiceImpl(Oauth2Configuration configuration) {
        this.configuration = configuration;
        credentialProvider = new PairedProviderWrapper(configuration.getPrivateKeyResolver(), configuration.getCertificateResolver(), configuration.getPrivateKeyAlias());

        RSAKeyProvider verifierProvider = new PublicKeyProviderWrapper(configuration.getIdentityProviderKeyResolver());
        verifier = JWT.require(Algorithm.RSA256(verifierProvider)).build();
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        String assertion = buildJwt(configuration.getProviderAudience());

        RequestBody requestBody = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", scope)
                .build();

        Request request = new Request.Builder().url(configuration.getTokenUrl()).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        OkHttpClient client = createClient();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (var body = response.body()) {
                    String message = body == null ? "<empty body>" : body.string();
                    return TokenResult.Builder.newInstance().error(message).build();
                }
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return TokenResult.Builder.newInstance().error("<empty token body>").build();
            }

            String responsePayload = responseBody.string();
            LinkedHashMap<String, Object> deserialized = configuration.getObjectMapper().readValue(responsePayload, LinkedHashMap.class);
            String token = (String) deserialized.get("access_token");
            long expiresIn = ((Integer) deserialized.get("expires_in")).longValue();
            return TokenResult.Builder.newInstance().token(token).expiresIn(expiresIn).build();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        try {
            var jwt = verifier.verify(token);
            return validateToken(jwt, audience);
        } catch (JWTVerificationException e) {
            return new VerificationResult(e.getMessage());
        }
    }

    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     */
    @NotNull
    VerificationResult validateToken(DecodedJWT jwt, String audience) {
        if (jwt.getAudience() == null) {
            return new VerificationResult("Token audience was empty");
        }
        if (jwt.getAudience().stream().noneMatch(audience::equals)) {
            return new VerificationResult("Token audience did not match required audience: " + audience);
        }

        var nowUtc = Instant.now();

        if (jwt.getNotBefore() == null) {
            return new VerificationResult("Token not before value was empty");
        }
        var nbf = ZonedDateTime.ofInstant(jwt.getNotBefore().toInstant(), ZoneId.of("UTC")).toInstant();
        if (nowUtc.isBefore(nbf)) {
            return new VerificationResult("Token not before is after current UTC time");
        }

        if (jwt.getExpiresAt() == null) {
            return new VerificationResult("Token expiration value was empty");
        }
        var expires = ZonedDateTime.ofInstant(jwt.getExpiresAt().toInstant(), ZoneId.of("UTC")).toInstant();
        if (!nowUtc.isBefore(expires)) {
            return new VerificationResult("Token has expired");
        }

        var tokenBuilder = ClaimToken.Builder.newInstance();
        jwt.getClaims().forEach((k, v) -> {
            var claimValue = v.asString();
            if (claimValue == null) {
                // only support strings
                return;
            }
            tokenBuilder.claim(k, claimValue);
        });
        return new VerificationResult(tokenBuilder.build());
    }

    private String buildJwt(String providerAudience) {
        try {
            X509Certificate certificate = configuration.getCertificateResolver().resolveCertificate(configuration.getPublicCertificateAlias());
            if (certificate == null) {
                throw new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias());
            }

            JWTCreator.Builder jwtBuilder = JWT.create();

            jwtBuilder.withHeader(Map.of("x5t", sha1Base64Fingerprint(certificate.getEncoded())));

            jwtBuilder.withAudience(providerAudience);

            jwtBuilder.withIssuer(configuration.getClientId());
            jwtBuilder.withSubject(configuration.getClientId());
            jwtBuilder.withJWTId(UUID.randomUUID().toString());
            jwtBuilder.withNotBefore(new Date());
            jwtBuilder.withExpiresAt(Date.from(Instant.now().plusSeconds(EXPIRATION)));

            return jwtBuilder.sign(Algorithm.RSA256(credentialProvider));
        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    }

}
