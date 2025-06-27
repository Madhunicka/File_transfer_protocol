package crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class CryptoUtils {
    public static byte[] encryptAES(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return c.doFinal(data);
    }

    public static byte[] decryptAES(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return c.doFinal(data);
    }

    public static byte[] encryptRSA(byte[] data, PublicKey pk) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, pk);
        return c.doFinal(data);
    }

    public static byte[] decryptRSA(byte[] data, PrivateKey sk) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.DECRYPT_MODE, sk);
        return c.doFinal(data);
    }

    public static byte[] computeHash(byte[] data) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        return d.digest(data);
    }

    public static byte[] signHash(byte[] hash, PrivateKey sk) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(sk);
        s.update(hash);
        return s.sign();
    }

    public static boolean verifySignature(byte[] hash, byte[] sigBytes, PublicKey pk) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(pk);
        s.update(hash);
        return s.verify(sigBytes);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
