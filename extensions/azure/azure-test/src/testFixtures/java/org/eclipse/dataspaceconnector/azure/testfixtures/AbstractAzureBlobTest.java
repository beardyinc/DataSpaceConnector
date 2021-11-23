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

package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.azure.core.credential.AzureSasCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractAzureBlobTest {

    protected static final String ACCOUNT_NAME = "storageitest";
    protected static BlobServiceClient blobServiceClient;
    protected String containerName;
    protected boolean reuseClient = true;
    protected String testRunId;

    @BeforeEach
    public void setupClient() {

        testRunId = UUID.randomUUID().toString();
        containerName = "storage-container-" + testRunId;

        if (blobServiceClient == null || !reuseClient) {
            var accountSas = getSasToken();
            blobServiceClient = new BlobServiceClientBuilder().credential(new AzureSasCredential(accountSas)).endpoint("https://" + ACCOUNT_NAME + ".blob.core.windows.net").buildClient();
        }

        if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
            fail("Container " + containerName + " already exists - tests  will fail!");
        }

        //create container
        BlobContainerClient blobContainerClient = blobServiceClient.createBlobContainer(containerName);
        if (!blobContainerClient.exists()) {
            fail("Setup incomplete, tests will fail");

        }
    }

    @AfterEach
    public void teardown() {
        try {
            blobServiceClient.deleteBlobContainer(containerName);
        } catch (Exception ex) {
            fail("teardown failed, subsequent tests might fail as well!");
        }
    }

    @NotNull
    protected String getSasToken() {
        return Objects.requireNonNull(propOrEnv("AZ_STORAGE_SAS", null), "AZ_STORAGE_SAS");
    }

    protected void putBlob(String name, File file) {
        blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
