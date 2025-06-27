package crypto;

import ui.UILogger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

public class FileSender {
    public static void sendFile(File inputFile, UILogger logger) {
        try (Socket socket = new Socket("localhost", 6000);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            byte[] fileBytes = Files.readAllBytes(inputFile.toPath());
            logger.log("[Sender] Original file content: " + new String(fileBytes));
            logger.log("[Sender] Original file (Base64): " + Base64.getEncoder().encodeToString(fileBytes));

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            byte[] nonce = new byte[16];
            new SecureRandom().nextBytes(nonce);

            byte[] encryptedFile = CryptoUtils.encryptAES(fileBytes, KeyManager.symmetricKey, iv);
            logger.log("[Sender] IV: " + Base64.getEncoder().encodeToString(iv));
            logger.log("[Sender] Nonce: " + Base64.getEncoder().encodeToString(nonce));
            logger.log("[Sender] Encrypted File: " + Base64.getEncoder().encodeToString(encryptedFile));

            byte[] encryptedSymKey = CryptoUtils.encryptRSA(KeyManager.symmetricKey.getEncoded(), KeyManager.receiverKeyPair.getPublic());
            logger.log("[Sender] Encrypted AES Key: " + Base64.getEncoder().encodeToString(encryptedSymKey));

            // Create metadata + content block for signing
            ByteArrayOutputStream metaOut = new ByteArrayOutputStream();
            metaOut.write(inputFile.getName().getBytes());
            metaOut.write(iv);
            metaOut.write(nonce);
            metaOut.write(fileBytes);
            byte[] metaDataContent = metaOut.toByteArray();

            byte[] fileHash = CryptoUtils.computeHash(metaDataContent);
            logger.log("[Sender] SHA-256 Hash (Base64): " + Base64.getEncoder().encodeToString(fileHash));
            logger.log("[Sender] SHA-256 Hash (Hex): " + CryptoUtils.bytesToHex(fileHash));

            byte[] sig = CryptoUtils.signHash(fileHash, KeyManager.senderKeyPair.getPrivate());
            logger.log("[Sender] Digital Signature: " + Base64.getEncoder().encodeToString(sig));

            // Send metadata and encrypted data
            dos.writeUTF(inputFile.getName());
            dos.writeInt(iv.length); dos.write(iv);
            dos.writeInt(nonce.length); dos.write(nonce);
            dos.writeInt(encryptedFile.length); dos.write(encryptedFile);
            dos.writeInt(encryptedSymKey.length); dos.write(encryptedSymKey);
            dos.writeInt(sig.length); dos.write(sig);

            logger.log("[Sender] File sent successfully.");
        } catch (Exception e) {
            logger.log("[Sender] Error: " + e.getMessage());
        }
    }
}