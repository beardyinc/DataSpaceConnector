package org.eclipse.dataspaceconnector.ion.crypto;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.ion.IonCryptoException;

public class EcPrivateKeyWrapper implements PrivateKeyWrapper {
    private final ECKey privateKey;

    public EcPrivateKeyWrapper(ECKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public JWEDecrypter decrypter() {
        try {
            return new ECDHDecrypter(privateKey);
        } catch (JOSEException e) {
            throw new IonCryptoException(e);
        }
    }

    @Override
    public JWSSigner signer() {
        try {
            return new ECDSASigner(privateKey);
        } catch (JOSEException e) {
            throw new IonCryptoException(e);
        }
    }
}
