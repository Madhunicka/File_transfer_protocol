package crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class KeyManager {
    public static KeyPair senderKeyPair;
    public static KeyPair receiverKeyPair;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            senderKeyPair = kpg.generateKeyPair();
            receiverKeyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
