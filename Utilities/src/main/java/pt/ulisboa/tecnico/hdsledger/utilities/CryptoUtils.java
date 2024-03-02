package pt.ulisboa.tecnico.hdsledger.utilities;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.logging.Level;

public class CryptoUtils {

    private static final CustomLogger LOGGER = new CustomLogger(CryptoUtils.class.getName());

    public static PrivateKey parsePrivateKey(String key){
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "")));
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.KeyParsingFailed);
        }
    }

    public static PublicKey parsePublicKey(String key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "")));
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.KeyParsingFailed);
        }
    }

    public static String generateSignature(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);

            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, MessageFormat.format("Signature Generator Error: {0}", e.getMessage()));
            throw new HDSSException(ErrorMessage.SignFailed);
        }
    }

    public static boolean verifySignature(byte[] data, String incomingSignature, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);

            return signature.verify(Base64.getDecoder().decode(incomingSignature));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, MessageFormat.format("Signature Validation Error: {0}", e.getMessage()));
            return false;
        }
    }
}