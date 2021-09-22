/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did;

import com.nimbusds.jose.JOSEException;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolver.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RSAPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.dataspaceconnector.iam.did.keys.TemporaryKeyLoader.loadKeys;


public class IdentityDidCoreHubExtension implements ServiceExtension {
    @EdcSetting
    private static final String RESOLVER_URL_KEY = "edc.did.resolver.url";

    private static final String RESOLVER_URL = "http://gx-ion-node.westeurope.cloudapp.azure.com:3000/identifiers/";

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "edc.did.private.key.alias";

    @Override
    public Set<String> provides() {
        return Set.of(IdentityHub.FEATURE, IdentityHubClient.FEATURE, DidResolver.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityHubStore.FEATURE, IonClient.FEATURE, PrivateKeyResolver.FEATURE, DidPublicKeyResolver.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var hubStore = context.getService(IdentityHubStore.class);

        var objectMapper = context.getTypeManager().getMapper();

        var publicKeyResolver = context.getService(DidPublicKeyResolver.class);
        var privateKeyResolver = context.getService(PrivateKeyResolver.class);

        PrivateKeyWrapper privateKeyWrapper = privateKeyResolver.resolvePrivateKey(context.getConnectorId(), PrivateKeyWrapper.class);
        Supplier<PrivateKeyWrapper> supplier = () -> privateKeyWrapper;
        var hub = new IdentityHubImpl(hubStore, supplier, publicKeyResolver, objectMapper);
        context.registerService(IdentityHub.class, hub);

        var controller = new IdentityHubController(hub);
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        var httpClient = context.getService(OkHttpClient.class);
        var ionClient = context.getService(IonClient.class);
        context.registerService(DidResolver.class, ionClient);

        var hubClient = new IdentityHubClientImpl(supplier, httpClient, objectMapper);
        context.registerService(IdentityHubClient.class, hubClient);

        context.getMonitor().info("Initialized Identity Did Core extension");
    }

    // TODO: TEMPORARY local key management to be replaced by vault storage
    private ResolverPair temporaryLoadResolvers(ServiceExtensionContext context) {
        try {
            var keys = loadKeys(context.getMonitor());
            if (keys == null) {
                return new ResolverPair(() -> null, (dis) -> null);
            }
            PublicKey publicKey = keys.toPublicKey();
            RSAPrivateKey privateKey = keys.toRSAPrivateKey();

            var privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
            var privateKeyResolver = context.getService(PrivateKeyResolver.class);

            Supplier<PrivateKeyWrapper> supplier = () -> new RSAPrivateKeyWrapper(privateKeyResolver.resolvePrivateKey(privateKeyAlias, RSAPrivateKey.class));

            DidPublicKeyResolver publicKeyResolver = new DidPublicKeyResolverImpl(publicKey);
            return new ResolverPair(supplier, publicKeyResolver);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }


    private static class ResolverPair {
        Supplier<PrivateKeyWrapper> privateKeySupplier;
        DidPublicKeyResolver publicKeyResolver;

        public ResolverPair(Supplier<PrivateKeyWrapper> privateKeySupplier, DidPublicKeyResolver publicKeyResolver) {
            this.privateKeySupplier = privateKeySupplier;
            this.publicKeyResolver = publicKeyResolver;
        }
    }
}
