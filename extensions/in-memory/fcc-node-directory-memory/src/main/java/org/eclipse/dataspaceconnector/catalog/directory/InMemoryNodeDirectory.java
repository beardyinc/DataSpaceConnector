package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryNodeDirectory implements FederatedCacheNodeDirectory {
    private final List<FederatedCacheNode> cache = new CopyOnWriteArrayList<>();

    @Override
    public List<FederatedCacheNode> getAll() {
        return List.copyOf(cache); //never return the internal copy
    }

    @Override
    public void insert(FederatedCacheNode node) {
        cache.add(node);
    }
}
