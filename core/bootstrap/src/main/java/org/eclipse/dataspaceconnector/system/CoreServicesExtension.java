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

package org.eclipse.dataspaceconnector.system;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.system.health.HealthCheckServiceConfiguration;
import org.eclipse.dataspaceconnector.system.health.HealthCheckServiceImpl;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CoreServicesExtension implements ServiceExtension {

    public static final String FEATURE_HTTP_CLIENT = "dataspaceconnector:http-client";
    public static final String FEATURE_RETRY_POLICY = "edc:retry-policy";

    @EdcSetting
    public static final String MAX_RETRIES = "edc.core.retry.retries.max";

    @EdcSetting
    public static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";

    @EdcSetting
    public static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";

    @EdcSetting
    public static final String LIVENESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.liveness-period";
    @EdcSetting
    public static final String STARTUP_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.startup-period";
    @EdcSetting
    public static final String READINESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.readiness-period";
    @EdcSetting
    public static final String THREADPOOL_SIZE_SETTING = "edc.core.system.health.check.threadpool-size";
    private static final String DEFAULT_DURATION = "60";
    private static final String DEFAULT_TP_SIZE = "3";


    private HealthCheckServiceImpl healthCheckService;

    @Override
    public String name() {
        return "Core Services";
    }

    @Override
    public Set<String> provides() {
        // the PrivateKeyResolver.FEATURE is not required because it gets registered directly by the
        // ExtensionLoader.
        return Set.of(FEATURE_HTTP_CLIENT, FEATURE_RETRY_POLICY, HealthCheckService.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        addHttpClient(context);
        addRetryPolicy(context);
        registerParser(context);
        var config = getHealthCheckConfig(context);
        healthCheckService = new HealthCheckServiceImpl(config);
        context.registerService(HealthCheckService.class, healthCheckService);
    }

    @Override
    public void shutdown() {
        healthCheckService.stop();
        ServiceExtension.super.shutdown();
    }

    private HealthCheckServiceConfiguration getHealthCheckConfig(ServiceExtensionContext context) {

        return HealthCheckServiceConfiguration.Builder.newInstance()
                .livenessPeriod(Duration.ofSeconds(Long.parseLong(context.getSetting(LIVENESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION))))
                .startupStatusPeriod(Duration.ofSeconds(Long.parseLong(context.getSetting(STARTUP_PERIOD_SECONDS_SETTING, DEFAULT_DURATION))))
                .readinessPeriod(Duration.ofSeconds(Long.parseLong(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION))))
                .readinessPeriod(Duration.ofSeconds(Long.parseLong(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION))))
                .threadPoolSize(Integer.parseInt(context.getSetting(THREADPOOL_SIZE_SETTING, DEFAULT_TP_SIZE)))
                .build();
    }

    private void registerParser(ServiceExtensionContext context) {
        var resolver = context.getService(PrivateKeyResolver.class);
        resolver.addParser(RSAPrivateKey.class, encoded -> {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded.getBytes())));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new EdcException(e);
            }
        });
    }


    private void addRetryPolicy(ServiceExtensionContext context) {

        var maxRetries = Integer.parseInt(context.getSetting(MAX_RETRIES, String.valueOf(5)));
        var minBackoff = Integer.parseInt(context.getSetting(BACKOFF_MIN_MILLIS, String.valueOf(500)));
        var maxBackoff = Integer.parseInt(context.getSetting(BACKOFF_MAX_MILLIS, String.valueOf(10_000)));

        var retryPolicy = new RetryPolicy<>()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS);

        context.registerService(RetryPolicy.class, retryPolicy);

    }

    private void addHttpClient(ServiceExtensionContext context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS);
        //        if (interceptors != null) {
        //           for (Interceptor interceptor : interceptors) {
        //                builder.addInterceptor(interceptor);
        //            }
        //        }
        var client = builder.build();

        context.registerService(OkHttpClient.class, client);
    }
}
