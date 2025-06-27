package crypto;

import ui.UILogger;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class FileReceiver {

    private static final Set<String> recentNonces = new HashSet<>();
    private static final long MAX_TIMESTAMP_AGE_MS = 5 * 60 * 1000; // 5 minutes max age

    private static File lastReceivedFile = null;
    private static String lastReceivedFileName = null;

    public static File getLastReceivedFile() {
        return lastReceivedFile;
    }

    public static String getLastReceivedFileName() {
        return lastReceivedFileName;
    }

    public static void clearLastReceivedFile() {
        lastReceivedFile = null;
        lastReceivedFileName = null;
    }

    public static void startReceiver(UILogger logger, Runnable enableSaveButton) {
        try (ServerSocket ss = new ServerSocket(6000)) {
            logger.log("[Receiver] Listening on port 6000...");

            while (true) {
                try (Socket sock = ss.accept();
                     DataInputStream dis = new DataInputStream(sock.getInputStream())) {

                    logger.log("[Receiver] Connection accepted, reading data...");

                    // Read ephemeral public key
                    int ephKeyLen = dis.readInt();
                    byte[] ephKeyBytes = new byte[ephKeyLen];
                    dis.readFully(ephKeyBytes);
                    KeyFactory kf = KeyFactory.getInstance("EC");
                    PublicKey ephemeralPubKey = kf.generatePublic(new X509EncodedKeySpec(ephKeyBytes));
                    logger.log("[Receiver] Ephemeral public key received (Base64): " + Base64.getEncoder().encodeToString(ephKeyBytes));

                    // Read nonce
                    int nonceLen = dis.readInt();
                    byte[] nonce = new byte[nonceLen];
                    dis.readFully(nonce);
                    logger.log("[Receiver] Nonce received (Base64): " + Base64.getEncoder().encodeToString(nonce));

                    // Read timestamp
                    long timestamp = dis.readLong();
                    logger.log("[Receiver] Timestamp received (ms): " + timestamp);

                    // Validate timestamp freshness
                    long now = System.currentTimeMillis();
                    if (Math.abs(now - timestamp) > MAX_TIMESTAMP_AGE_MS) {
                        logger.log("[Receiver] Timestamp too old or invalid, rejecting.");
                        continue;
                    }

                    // Replay protection: Check nonce uniqueness
                    String nonceBase64 = Base64.getEncoder().encodeToString(nonce);
                    synchronized (recentNonces) {
                        if (recentNonces.contains(nonceBase64)) {
                            logger.log("[Receiver] Replay detected for nonce: " + nonceBase64);
                            continue;
                        }
                        recentNonces.add(nonceBase64);
                    }
                    logger.log("[Receiver] Nonce accepted for replay protection.");

                    // Read IV for AES-GCM
                    int ivLen = dis.readInt();
                    byte[] iv = new byte[ivLen];
                    dis.readFully(iv);
                    logger.log("[Receiver] IV received (Base64): " + Base64.getEncoder().encodeToString(iv));

                    // Read encrypted payload
                    int encryptedLen = dis.readInt();
                    byte[] encryptedPayload = new byte[encryptedLen];
                    dis.readFully(encryptedPayload);
                    logger.log("[Receiver] Encrypted payload received (size: " + encryptedPayload.length + " bytes).");

                    // Read signature
                    int sigLen = dis.readInt();
                    byte[] signature = new byte[sigLen];
                    dis.readFully(signature);
                    logger.log("[Receiver] Signature received (Base64): " + Base64.getEncoder().encodeToString(signature));

                    // Reconstruct data for signature verification
                    ByteArrayOutputStream signStream = new ByteArrayOutputStream();
                    DataOutputStream signDos = new DataOutputStream(signStream);

                    signDos.writeInt(ephKeyBytes.length);
                    signDos.write(ephKeyBytes);

                    signDos.writeInt(nonce.length);
                    signDos.write(nonce);

                    signDos.writeLong(timestamp);

                    signDos.writeInt(iv.length);
                    signDos.write(iv);

                    signDos.writeInt(encryptedPayload.length);
                    signDos.write(encryptedPayload);

                    byte[] dataToVerify = signStream.toByteArray();

                    // Verify signature
                    boolean validSig = CryptoUtils.verifySignature(dataToVerify, signature, KeyManager.senderKeyPair.getPublic());
                    if (!validSig) {
                        logger.log("[Receiver] Signature verification failed, rejecting.");
                        continue;
                    }
                    logger.log("[Receiver] Signature verified successfully.");

                    // Derive shared secret via ECDH
                    byte[] sharedSecret = CryptoUtils.deriveSharedSecret(
                            KeyManager.receiverKeyPair.getPrivate(), ephemeralPubKey);
                    logger.log("[Receiver] Shared secret derived (Base64): " + Base64.getEncoder().encodeToString(sharedSecret));

                    // Derive AES key using nonce and timestamp
                    SecretKey aesKey = CryptoUtils.deriveAESKey(sharedSecret, nonce, timestamp);
                    logger.log("[Receiver] AES key derived (Base64): " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));

                    //Decrypt payload
                    byte[] decryptedPayload = CryptoUtils.decryptAESGCM(encryptedPayload, aesKey, iv);
                    logger.log("[Receiver] Payload decrypted successfully (size: " + decryptedPayload.length + " bytes).");

                    // Extract file name and content
                    DataInputStream payloadDis = new DataInputStream(new ByteArrayInputStream(decryptedPayload));
                    String fileName = payloadDis.readUTF();
                    int fileLength = payloadDis.readInt();
                    byte[] fileBytes = new byte[fileLength];
                    payloadDis.readFully(fileBytes);

                    // Save file temporarily
                    File receivedFile = new File("received_" + fileName);
                    Files.write(receivedFile.toPath(), fileBytes);

                    lastReceivedFile = receivedFile;
                    lastReceivedFileName = fileName;

//                    logger.log("[Receiver] File received and saved: " + receivedFile.getAbsolutePath());

                    // Enable save button in UI
                    enableSaveButton.run();

                } catch (Exception e) {
                    logger.log("[Receiver] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            logger.log("[Receiver] Server socket error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.log("[Receiver] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
