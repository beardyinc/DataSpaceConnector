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

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Default provision manager. Invoke {@link #start(TransferProcessStore)} to initialize an instance.
 */
public class ProvisionManagerImpl implements ProvisionManager {
    private final Vault vault;
    private final TypeManager typeManager;
    private final Monitor monitor;
    private final List<Provisioner<?, ?>> provisioners = new ArrayList<>();
    private TransferProcessStore processStore;

    public ProvisionManagerImpl(Vault vault, TypeManager typeManager, Monitor monitor) {
        this.vault = vault;
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    public void start(TransferProcessStore processStore) {
        this.processStore = processStore;
        var context = new ProvisionContextImpl(this.processStore, this::onResource, this::onDestinationResource, this::onDeprovisionComplete);
        provisioners.forEach(provisioner -> provisioner.initialize(context));
    }

    @Override
    public <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public void provision(TransferProcess process) {
        if (process.getResourceManifest().getDefinitions().isEmpty()) {
            // no resources to provision, advance state
            process.transitionProvisioned();
            processStore.update(process);
        }
        for (ResourceDefinition definition : process.getResourceManifest().getDefinitions()) {
            Provisioner<ResourceDefinition, ?> chosenProvisioner = getProvisioner(definition);
            var status = chosenProvisioner.provision(definition);
        }
    }

    @Override
    public void deprovision(TransferProcess process) {
        for (ProvisionedResource definition : process.getProvisionedResourceSet().getResources()) {
            Provisioner<?, ProvisionedResource> chosenProvisioner = getProvisioner(definition);
            ResponseStatus status = chosenProvisioner.deprovision(definition);
            if (status != ResponseStatus.OK) {
                process.transitionError("Error during deprovisioning");
                processStore.update(process);
            }
        }
    }

    void onDeprovisionComplete(ProvisionedDataDestinationResource resource, Throwable deprovisionError) {
        if (deprovisionError != null) {
            monitor.severe("Deprovisioning error: ", deprovisionError);
        } else {
            monitor.info("Deprovisioning successfully completed.");

            TransferProcess transferProcess = processStore.find(resource.getTransferProcessId());
            if (transferProcess != null) {
                transferProcess.transitionDeprovisioned();
                processStore.update(transferProcess);
                monitor.debug("Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
            } else {
                monitor.severe("ProvisionManager: no TransferProcess found for deprovisioned resource");
            }

        }
    }

    void onDestinationResource(ProvisionedDataDestinationResource destinationResource, SecretToken secretToken) {
        var processId = destinationResource.getTransferProcessId();
        var transferProcess = processStore.find(processId);
        if (transferProcess == null) {
            processNotFound(destinationResource);
            return;
        }

        if (!destinationResource.isError()) {
            transferProcess.getDataRequest().updateDestination(destinationResource.createDataDestination());
        }

        if (secretToken != null) {
            String keyName = destinationResource.getResourceName();
            vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
            transferProcess.getDataRequest().getDataDestination().setKeyName(keyName);

        }

        updateProcessWithProvisionedResource(destinationResource, transferProcess);
    }

    void onResource(ProvisionedResource provisionedResource) {
        var processId = provisionedResource.getTransferProcessId();
        var transferProcess = processStore.find(processId);
        if (transferProcess == null) {
            processNotFound(provisionedResource);
            return;
        }

        updateProcessWithProvisionedResource(provisionedResource, transferProcess);
    }

    private void updateProcessWithProvisionedResource(ProvisionedResource provisionedResource, TransferProcess transferProcess) {
        transferProcess.addProvisionedResource(provisionedResource);

        if (provisionedResource.isError()) {
            var processId = transferProcess.getId();
            var resourceId = provisionedResource.getResourceDefinitionId();
            monitor.severe(format("Error provisioning resource %s for process %s: %s", resourceId, processId, provisionedResource.getErrorMessage()));
            processStore.update(transferProcess);
            return;
        }

        if (TransferProcessStates.ERROR.code() != transferProcess.getState() && transferProcess.provisioningComplete()) {
            // TODO If all resources provisioned, delete scratch data
            transferProcess.transitionProvisioned();
        }
        processStore.update(transferProcess);
    }

    private void processNotFound(ProvisionedResource provisionedResource) {
        var resourceId = provisionedResource.getResourceDefinitionId();
        var processId = provisionedResource.getTransferProcessId();
        monitor.severe(format("Error received when provisioning resource %s Process id not found for: %s", resourceId, processId));
    }

    @NotNull
    private Provisioner<ResourceDefinition, ?> getProvisioner(ResourceDefinition definition) {
        Provisioner<ResourceDefinition, ?> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canProvision(definition)) {
                provisioner = (Provisioner<ResourceDefinition, ?>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new EdcException("Unknown provision type" + definition.getClass().getName());
        }
        return provisioner;
    }

    @NotNull
    private Provisioner<?, ProvisionedResource> getProvisioner(ProvisionedResource provisionedResource) {
        Provisioner<?, ProvisionedResource> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canDeprovision(provisionedResource)) {
                provisioner = (Provisioner<?, ProvisionedResource>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new EdcException("Unknown provision type" + provisionedResource.getClass().getName());
        }
        return provisioner;
    }


}
