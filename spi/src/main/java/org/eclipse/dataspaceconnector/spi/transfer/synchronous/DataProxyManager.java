package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * This manager is used on the provider-side of a synchronous data transfer to maintain a list of {@link DataProxy} instances.
 */
public interface DataProxyManager {
    String FEATURE = "edc:transfer:dataproxymanager";

    void addProxy(String type, DataProxy proxy);

    /**
     * Gets a {@link DataProxy} for a particular DataRequest. If none is found, {@code null} is returned.
     */
    DataProxy getProxy(DataRequest dataRequest);
}
