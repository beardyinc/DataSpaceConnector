package org.eclipse.dataspaceconnector.iam.did.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.iam.did.util.GaiaxAssumptions.assumptions;


class DidResolverImplTest {
    private static final String RESOLVER_URL = "http://gx-ion-node.westeurope.cloudapp.azure.com:3000/identifiers/";

    @Test
    void verifyResolution() {
        assumptions();
        var resolver = new DidResolverImpl(RESOLVER_URL, new OkHttpClient(), new ObjectMapper());
        var did = resolver.resolveDid("did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");
        Assertions.assertNotNull(did);
    }
}
