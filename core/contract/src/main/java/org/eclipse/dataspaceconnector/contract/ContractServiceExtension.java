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
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class ContractServiceExtension implements ServiceExtension {
    private static final String NAME = "Core Contract Service Extension";
    private static final String[] PROVIDES = {
            ContractOfferService.class.getName()
    };

    private Monitor monitor;

    @Override
    public final Set<String> provides() {
        return Set.of(PROVIDES);
    }

    @Override
    public final Set<String> requires() {
        return Set.of(AssetIndex.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerServices(serviceExtensionContext);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }

    private void registerServices(ServiceExtensionContext serviceExtensionContext) {

        AssetIndex assetIndex = serviceExtensionContext.getService(AssetIndex.class);

        /*
         * Construct a ContractOfferFrameworkLocator for finding several ContractOfferFrameworks provided via extensions
         */
        ContractOfferFrameworkLocator compositeContractOfferFramework = new ContractOfferFrameworkLocator(
                serviceExtensionContext
        );
        /*
         * There is always one default contract offer framework instance
         * delegating calls to those provided by custom extensions.
         */
        ContractOfferFramework contractOfferFramework = new CompositeContractOfferFramework(
                compositeContractOfferFramework,
                serviceExtensionContext.getMonitor());
        /*
         * Contract offer service calculates contract offers using a variety of contract offer frameworks
         * ad the given asset index.
         */
        ContractOfferService contractOfferService = new ContractOfferServiceImpl(
                contractOfferFramework,
                assetIndex
        );
        /*
         * Register the just created contract offer service to the service extension context.
         */
        serviceExtensionContext.registerService(ContractOfferService.class, contractOfferService);
    }
}
