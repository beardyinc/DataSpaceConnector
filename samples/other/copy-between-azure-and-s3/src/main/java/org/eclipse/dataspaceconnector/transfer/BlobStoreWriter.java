package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.io.InputStream;

class BlobStoreWriter implements DataWriter {
    public BlobStoreWriter(Monitor monitor, TypeManager typeManager) {

    }

    @Override
    public void write(DataAddress destination, String name, InputStream data, String secretToken) {
        throw new UnsupportedOperationException("this operation is not yet implemented!");
    }
}
