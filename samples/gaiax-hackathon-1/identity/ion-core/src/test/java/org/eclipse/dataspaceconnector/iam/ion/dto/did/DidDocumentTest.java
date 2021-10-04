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
package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 *
 */
class DidDocumentTest {
    private ObjectMapper objectMapper;

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var serviceEndpoint = new ServiceEndpoint("someschema", "SomeEndpoint", List.of("https://test.service.com"));
        var service = new Service("#domain-1", "LinkedDomains", serviceEndpoint);
        var document = DidDocument.Builder.newInstance().id("did:ion:123").service(service).build();
        var serialized = objectMapper.writeValueAsString(document);
        var deserialized = objectMapper.readValue(serialized, DidDocument.class);
        Assertions.assertEquals("did:ion:123", deserialized.getId());
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
}
