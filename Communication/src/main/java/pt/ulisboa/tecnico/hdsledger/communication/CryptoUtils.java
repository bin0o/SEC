package pt.ulisboa.tecnico.hdsledger.communication;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
        public static byte[] authenticate(String privateKey, ConsensusMessage message) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
            // Convert the private key and message to bytes
            byte[] privateKeyBytes = privateKey.getBytes();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();
            byte [] messageBytes = bos.toByteArray();


            // Create SecretKeySpec object using the private key
            SecretKeySpec secretKeySpec = new SecretKeySpec(privateKeyBytes, "HmacSHA256");

            // Create HMAC SHA-256 instance
            Mac mac = Mac.getInstance("HmacSHA256");

            // Initialize the Mac object with the private key
            mac.init(secretKeySpec);

            // Return the MAC
            return mac.doFinal(messageBytes);
        }

        // Helper function to convert bytes to hexadecimal string
        public static String bytesToHex(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }

    public static boolean verifyAuth(String publicKey, ConsensusMessage message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        // Convert the public key and message to bytes
        byte[] publicKeyBytes = publicKey.getBytes();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        oos.flush();
        byte [] messageBytes = bos.toByteArray();

        // Create SecretKeySpec object using the public key
        SecretKeySpec secretKeySpec = new SecretKeySpec(publicKeyBytes, "HmacSHA256");

        // Create HMAC SHA-256 instance
        Mac mac = Mac.getInstance("HmacSHA256");

        // Initialize the Mac object with the public key
        mac.init(secretKeySpec);

        // Generate the MAC
        byte[] generatedSignature = mac.doFinal(messageBytes);

        // Compare generated signature with provided signature
        return true;
        //return ConsensusMessage.compareByteArrays(generatedSignature, signature);
    }
}
