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

package org.eclipse.dataspaceconnector.spi.metadata;

import org.eclipse.dataspaceconnector.spi.Observable;

public class MetadataObservable extends Observable<MetadataListener> {
    public static final String FEATURE = "dataspaceconnector:metadata-store-observable";
}
