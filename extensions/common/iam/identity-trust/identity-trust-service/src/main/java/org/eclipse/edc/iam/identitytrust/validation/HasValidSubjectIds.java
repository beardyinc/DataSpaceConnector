/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.validation;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class HasValidSubjectIds implements VcValidationRule {

    private final String expectedSubjectId;

    public HasValidSubjectIds(String expectedSubjectId) {

        this.expectedSubjectId = expectedSubjectId;
    }


    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        return credential.getCredentialSubject().stream().allMatch(sub -> sub.getId().equals(expectedSubjectId)) ?
                success() : failure("Not all subject IDs match the expected subject ID %s".formatted(expectedSubjectId));
    }
}
