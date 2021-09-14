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

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class S3DeprovisionPipeline {
    private final ClientProvider clientProvider;
    private final RetryPolicy<Object> retryPolicy;

    private final Monitor monitor;

    public S3DeprovisionPipeline(ClientProvider clientProvider, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.clientProvider = clientProvider;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    public void deprovision(S3BucketProvisionedResource resource, Consumer<Throwable> callback) {
        var s3Client = clientProvider.clientFor(S3AsyncClient.class, resource.getRegion());
        var iamClient = clientProvider.clientFor(IamAsyncClient.class, resource.getRegion());

        String bucketName = resource.getBucketName();

        monitor.info("S3 Deprovisioning: list bucket contents");
        String role = resource.getRole();
        s3Client.listObjectsV2(listBucket(bucketName))
                .thenCompose(listObjectsResponse -> {
                    //todo: collect identifiers into a single request using the stream api
                    var identifiers = listObjectsResponse.contents().stream()
                            .map(s3object -> ObjectIdentifier.builder().key(s3object.key()).build())
                            .collect(Collectors.toList());
                    var deleteRequest = DeleteObjectsRequest.builder().bucket(bucketName).delete(Delete.builder().objects(identifiers).build()).build();
                    monitor.info("S3 Deprovisioning: delete bucket contents: " + identifiers.stream().map(ObjectIdentifier::key).collect(Collectors.joining(", ")));
                    return s3Client.deleteObjects(deleteRequest);
                })
                .thenCompose(deleteObjectsResponse -> Failsafe.with(retryPolicy).getStageAsync(() -> {
                    monitor.info("S3 Deprovisioning: delete bucket");
                    return s3Client.deleteBucket(deleteBucket(bucketName));
                }))
                .thenCompose(listAttachedRolePoliciesResponse -> Failsafe.with(retryPolicy).getStageAsync(() -> {
                    monitor.info("S3 Deprovisioning: deleting inline policies for Role " + role);
                    return iamClient.deleteRolePolicy(DeleteRolePolicyRequest.builder().roleName(role).policyName(role).build());
                }))
                .thenCompose(deleteRolePolicyResponse -> Failsafe.with(retryPolicy).getStageAsync(() -> {
                    monitor.info("S3 Deprovisioning: delete role");
                    return iamClient.deleteRole(DeleteRoleRequest.builder().roleName(role).build());
                }))
                .whenComplete((deleteRoleResponse, throwable) -> callback.accept(throwable));
    }

    private DeleteBucketRequest deleteBucket(String bucketName) {
        return DeleteBucketRequest.builder().bucket(bucketName).build();
    }

    private ListObjectsV2Request listBucket(String bucketName) {
        return ListObjectsV2Request.builder().bucket(bucketName).build();
    }


    public static final class Builder {
        private ClientProvider clientProvider;
        private RetryPolicy<Object> retryPolicy;
        private Monitor monitor;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder clientProvider(ClientProvider clientProvider) {
            this.clientProvider = clientProvider;
            return this;
        }

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public S3DeprovisionPipeline build() {
            return new S3DeprovisionPipeline(clientProvider, retryPolicy, monitor);
        }

        public Builder resource() {
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }
    }
}
