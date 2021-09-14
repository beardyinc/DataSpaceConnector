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

package org.eclipse.dataspaceconnector.common.testfixtures;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
    public static final String SAMPLE_FILE_RESOURCE_NAME = "hello.txt";

    public static Path getResourcePath(String resourceName) {
        Path path = null;

        try {
            path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            fail("Resource: " + resourceName + " does not exist" + e.getLocalizedMessage());
        }

        return path;
    }

    public static File getFileFromResourceName(String resourceName) {
        URI uri = null;
        try {
            uri = Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI();
        } catch (URISyntaxException e) {
            fail("Cannot proceed without File : " + resourceName);
        }

        return new File(uri);
    }
}
