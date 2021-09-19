package org.eclipse.dataspaceconnector.ion.spi;

import org.eclipse.dataspaceconnector.ion.model.IonRequest;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidDocument;

public interface IonClient {

    String FEATURE = "edc:identity:ion:client";

    DidDocument submit(IonRequest request);

    DidDocument resolve(String identifier);
}
