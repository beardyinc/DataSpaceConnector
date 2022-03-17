/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.transformer;

import org.eclipse.dataspaceconnector.spi.transformer.TypeTransformer;
import org.jetbrains.annotations.NotNull;

/**
 * Marker interface to group transformers which are intended for use in API controllers, i.e. converting business objects
 * into DTOs
 *
 * @param <INPUT>  the type of input object. Usually this is a business object.
 * @param <OUTPUT> the type that the input gets converted into. Usually this is a DTO.
 */
public interface ApiTransformer<INPUT, OUTPUT> extends TypeTransformer<INPUT, OUTPUT> {
    /**
     * Determines whether a transformer can handle a particular pair.
     *
     * @param object     Source object
     * @param outputType desired target type.
     * @return true if can handle, false otherwise.
     */
    boolean canHandle(@NotNull Object object, @NotNull Class<?> outputType);
}
