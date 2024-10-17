/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspTransferProcessPropertyAndTypeNames {

    String DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM = "TransferRequestMessage";
    String DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
    String DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM = "TransferStartMessage";
    String DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
    String DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM = "TransferCompletionMessage";
    String DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
    String DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM = "TransferSuspensionMessage";
    String DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
    String DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM = "TransferTerminationMessage";
    String DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
    String DSPACE_TYPE_TRANSFER_PROCESS_TERM = "TransferProcess";
    String DSPACE_TYPE_TRANSFER_PROCESS_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_PROCESS_TERM;
    String DSPACE_TYPE_TRANSFER_ERROR_TERM = "TransferError";
    String DSPACE_TYPE_TRANSFER_ERROR_IRI = DSPACE_SCHEMA + DSPACE_TYPE_TRANSFER_ERROR_TERM;
    String DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM = "agreementId";
    String DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM;
    String DSPACE_PROPERTY_DATA_ADDRESS_TERM = "dataAddress";
    String DSPACE_PROPERTY_DATA_ADDRESS_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_DATA_ADDRESS_TERM;
}
