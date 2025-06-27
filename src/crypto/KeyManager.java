package crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Base64;

public class KeyManager {
    public static SecretKey symmetricKey;
    public static KeyPair senderKeyPair, receiverKeyPair;

    public static void generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            senderKeyPair = keyGen.generateKeyPair();
            receiverKeyPair = keyGen.generateKeyPair();

            System.out.println("=== RSA Keys Generated ===");
            System.out.println("[Sender Private Key] " + Base64.getEncoder().encodeToString(senderKeyPair.getPrivate().getEncoded()));
            System.out.println("[Sender Public Key] " + Base64.getEncoder().encodeToString(senderKeyPair.getPublic().getEncoded()));
            System.out.println("[Receiver Private Key] " + Base64.getEncoder().encodeToString(receiverKeyPair.getPrivate().getEncoded()));
            System.out.println("[Receiver Public Key] " + Base64.getEncoder().encodeToString(receiverKeyPair.getPublic().getEncoded()));

            KeyGenerator symKeyGen = KeyGenerator.getInstance("AES");
            symKeyGen.init(256);
            symmetricKey = symKeyGen.generateKey();

            System.out.println("[AES Symmetric Key] " + Base64.getEncoder().encodeToString(symmetricKey.getEncoded()));
        } catch (Exception e) {
            System.err.println("Error generating keys: " + e.getMessage());
        }
    }
}
