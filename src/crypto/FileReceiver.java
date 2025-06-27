package crypto;

import ui.UILogger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;

public class FileReceiver {
    public static String receivedFileName = "";
    private static File tempDecryptedFile = null;

    public static File getTempDecryptedFile() {
        return tempDecryptedFile;
    }

    public static void startReceiver(UILogger logger, Runnable enableSaveButton) {
        try (ServerSocket ss = new ServerSocket(6000)) {
            logger.log("[Receiver] Listening on port 6000...");
            while (true) {
                try (Socket sock = ss.accept();
                     DataInputStream dis = new DataInputStream(sock.getInputStream())) {

                    String fileName = dis.readUTF();
                    receivedFileName = fileName;
                    logger.log("[Receiver] File name: " + fileName);

                    byte[] iv = new byte[dis.readInt()]; dis.readFully(iv);
                    logger.log("[Receiver] IV: " + Base64.getEncoder().encodeToString(iv));

                    byte[] nonce = new byte[dis.readInt()]; dis.readFully(nonce);
                    logger.log("[Receiver] Nonce: " + Base64.getEncoder().encodeToString(nonce));

                    byte[] encryptedFile = new byte[dis.readInt()]; dis.readFully(encryptedFile);
                    logger.log("[Receiver] Encrypted File: " + Base64.getEncoder().encodeToString(encryptedFile));

                    byte[] encryptedKey = new byte[dis.readInt()]; dis.readFully(encryptedKey);
                    logger.log("[Receiver] Encrypted AES Key: " + Base64.getEncoder().encodeToString(encryptedKey));

                    byte[] sig = new byte[dis.readInt()]; dis.readFully(sig);
                    logger.log("[Receiver] Digital Signature: " + Base64.getEncoder().encodeToString(sig));

                    byte[] aesBytes = CryptoUtils.decryptRSA(encryptedKey, KeyManager.receiverKeyPair.getPrivate());
                    SecretKey decryptedKey = new SecretKeySpec(aesBytes, "AES");
                    logger.log("[Receiver] Decrypted AES Key: " + Base64.getEncoder().encodeToString(decryptedKey.getEncoded()));

                    byte[] decryptedFile = CryptoUtils.decryptAES(encryptedFile, decryptedKey, iv);
                    logger.log("[Receiver] Decrypted File (Base64): " + Base64.getEncoder().encodeToString(decryptedFile));

                    // Build metadata + content to verify signature
                    ByteArrayOutputStream metaOut = new ByteArrayOutputStream();
                    metaOut.write(fileName.getBytes());
                    metaOut.write(iv);
                    metaOut.write(nonce);
                    metaOut.write(decryptedFile);
                    byte[] metaDataContent = metaOut.toByteArray();

                    byte[] hash = CryptoUtils.computeHash(metaDataContent);
                    logger.log("[Receiver] SHA-256 Hash (Base64): " + Base64.getEncoder().encodeToString(hash));
                    logger.log("[Receiver] SHA-256 Hash (Hex): " + CryptoUtils.bytesToHex(hash));

                    boolean ok = CryptoUtils.verifySignature(hash, sig, KeyManager.senderKeyPair.getPublic());
                    if (ok) {
                        logger.log("[Receiver] Signature verified.");
                        tempDecryptedFile = new File("temp_" + fileName);
                        Files.write(tempDecryptedFile.toPath(), decryptedFile);
                        enableSaveButton.run();
                    } else {
                        logger.log("[Receiver] Signature verification failed.");
                    }
                }
            }
        } catch (Exception e) {
            logger.log("[Receiver] Error: " + e.getMessage());
        }
    }
}