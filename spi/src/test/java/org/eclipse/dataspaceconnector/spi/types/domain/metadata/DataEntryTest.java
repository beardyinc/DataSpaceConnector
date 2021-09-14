/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEntryTest {

    @Test
    void verifyPolymorphicDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(TestExtension.class);

        DataEntry entry = DataEntry.Builder.newInstance().id("id").catalogEntry(new TestExtension()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entry);

        DataEntry deserialized = mapper.readValue(writer.toString(), DataEntry.class);

        assertNotNull(deserialized);
        assertTrue(deserialized.getCatalogEntry() instanceof TestExtension);
        assertEquals("id", deserialized.getId());
    }

    @JsonTypeName("dataspaceconnector:testextensions")
    public static class TestExtension implements DataCatalogEntry {

        @Override
        @JsonIgnore
        public DataAddress getAddress() {
            return null;
        }
    }
}
