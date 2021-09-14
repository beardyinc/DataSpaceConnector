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

package org.eclipse.dataspaceconnector.provision.aws.s3;

import net.jodah.failsafe.RetryPolicy;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.PutRolePolicyResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class S3BucketProvisionerTest {

    @Test
    void verifyBasicProvision() throws InterruptedException {

        var latch = new CountDownLatch(1);

        // get user
        var userFuture = new CompletableFuture<GetUserResponse>();

        IamAsyncClient iamMock = EasyMock.createMock(IamAsyncClient.class);
        iamMock.getUser();
        EasyMock.expectLastCall().andReturn(userFuture);

        // create role
        var roleFuture = new CompletableFuture<CreateRoleResponse>();
        EasyMock.expect(iamMock.createRole(EasyMock.isA(CreateRoleRequest.class))).andReturn(roleFuture);

        var putRoleFuture = new CompletableFuture<PutRolePolicyResponse>();
        EasyMock.expect(iamMock.putRolePolicy(EasyMock.isA(PutRolePolicyRequest.class))).andReturn(putRoleFuture);

        // assume the role
        StsAsyncClient stsMock = EasyMock.createMock(StsAsyncClient.class);
        var assumeRoleFuture = new CompletableFuture<AssumeRoleResponse>();
        EasyMock.expect(stsMock.assumeRole(EasyMock.isA(AssumeRoleRequest.class))).andReturn(assumeRoleFuture);

        // create the bucket
        S3AsyncClient s3Mock = EasyMock.createMock(S3AsyncClient.class);
        var s3Future = new CompletableFuture<CreateBucketResponse>();
        EasyMock.expect(s3Mock.createBucket(EasyMock.isA(CreateBucketRequest.class))).andReturn(s3Future);

        EasyMock.replay(iamMock, stsMock, s3Mock);

        ClientProvider clientProvider = mockProvider(iamMock, stsMock, s3Mock);

        S3BucketProvisioner provisioner = new S3BucketProvisioner(clientProvider, 3600, new Monitor() {
        }, new RetryPolicy<>());

        provisioner.initialize(new ProvisionContext() {
            @Override
            public void callback(ProvisionedResource resource) {
                throw new AssertionError("Resource creation errored");
            }

            @Override
            public void callback(ProvisionedDataDestinationResource resource, @Nullable SecretToken secretToken) {
                latch.countDown();
            }

            @Override
            public void deprovisioned(ProvisionedDataDestinationResource resource, Throwable error) {
                // noop here
            }

        });

        provisioner.provision(S3BucketResourceDefinition.Builder.newInstance().id("test").regionId(Region.US_EAST_1.id()).bucketName("test").transferProcessId("test").build());

        userFuture.complete(GetUserResponse.builder().user(User.builder().arn("testarn").build()).build());
        roleFuture.complete(CreateRoleResponse.builder().role(Role.builder().arn("testarn").build()).build());
        putRoleFuture.complete(PutRolePolicyResponse.builder().build());
        assumeRoleFuture.complete(AssumeRoleResponse.builder().credentials(Credentials.builder().expiration(Instant.now()).build()).build());
        s3Future.complete(CreateBucketResponse.builder().build());

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        EasyMock.verify(iamMock, stsMock, s3Mock);

    }

    private ClientProvider mockProvider(IamAsyncClient iamMock, StsAsyncClient stsMock, S3AsyncClient s3Mock) {
        return new ClientProvider() {
            @Override
            public <T extends SdkClient> T clientFor(Class<T> type, String key) {
                if (type.isAssignableFrom(S3AsyncClient.class)) {
                    return type.cast(s3Mock);
                } else if (type.isAssignableFrom(IamAsyncClient.class)) {
                    return type.cast(iamMock);
                } else if (type.isAssignableFrom(StsAsyncClient.class)) {
                    return type.cast(stsMock);
                }

                return type.cast(s3Mock);
            }
        };
    }
}
