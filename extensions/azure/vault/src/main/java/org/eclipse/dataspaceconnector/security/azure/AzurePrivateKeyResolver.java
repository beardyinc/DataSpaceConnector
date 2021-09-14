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

package org.eclipse.dataspaceconnector.security.azure;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Resolves RSA private keys stored as Base 64-encoded text against a vault.
 * <p>Keys must be PEM encoded in PKCS8 format. The key may be stored with or without PEM headers and footers.</p>
 */
public class AzurePrivateKeyResolver implements PrivateKeyResolver {
    private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";
    private final Vault vault;

    public AzurePrivateKeyResolver(Vault vault) {
        this.vault = vault;
    }

    @Override
    public RSAPrivateKey resolvePrivateKey(String id) {
        try {
            String encoded = vault.resolveSecret(id);
            if (encoded == null) {
                return null;
            }

            encoded = encoded.replace(PEM_HEADER, "").replaceAll(System.lineSeparator(), "").replace(PEM_FOOTER, "");
            encoded = encoded.replace("\n", ""); //base64 might complain if newlines are present

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded.getBytes())));
        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }
}
