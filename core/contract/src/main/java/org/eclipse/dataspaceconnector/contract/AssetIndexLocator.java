/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class AssetIndexLocator {
    private final ServiceExtensionContext serviceExtensionContext;

    public AssetIndexLocator(ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = serviceExtensionContext;
    }

    // TODO service extension should be able to load multiple instances of a given type
    public List<AssetIndex> getAssetIndexes() {
        return Optional.ofNullable(serviceExtensionContext.getService(AssetIndex.class, true))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }
}
