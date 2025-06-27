package crypto;

import ui.UILogger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;

public class FileSender {

    public static void sendFile(File inputFile, UILogger logger) {
        try (Socket socket = new Socket("localhost", 6000);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            logger.log("[Sender] Starting file send process...");

            //Generate ephemeral ECDH key pair
            KeyPair ephemeralKeyPair = CryptoUtils.generateECDHKeyPair();
            logger.log("[Sender] Ephemeral EC key pair generated.");
            logger.log("[Sender] Ephemeral Public Key (Base64): " + Base64.getEncoder().encodeToString(ephemeralKeyPair.getPublic().getEncoded()));

            //Derive shared secret from ephemeral private key and receiver public key
            byte[] sharedSecret = CryptoUtils.deriveSharedSecret(
                    ephemeralKeyPair.getPrivate(), KeyManager.receiverKeyPair.getPublic());
            logger.log("[Sender] Shared secret derived (Base64): " + Base64.getEncoder().encodeToString(sharedSecret));

            //Prepare nonce and timestamp
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            logger.log("[Sender] Nonce generated (Base64): " + Base64.getEncoder().encodeToString(nonce));
            long timestamp = System.currentTimeMillis();
            logger.log("[Sender] Timestamp (ms): " + timestamp);

            //Derive AES key from shared secret + nonce + timestamp
            SecretKey aesKey = CryptoUtils.deriveAESKey(sharedSecret, nonce, timestamp);
            logger.log("[Sender] AES key derived (Base64): " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));

            //Prepare plaintext payload: filename + file content
            byte[] fileBytes = Files.readAllBytes(inputFile.toPath());
            logger.log("[Sender] Read file '" + inputFile.getName() + "' of size " + fileBytes.length + " bytes.");

            ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();
            DataOutputStream payloadDos = new DataOutputStream(payloadStream);
            payloadDos.writeUTF(inputFile.getName());
            payloadDos.writeInt(fileBytes.length);
            payloadDos.write(fileBytes);
            byte[] plaintextPayload = payloadStream.toByteArray();

            logger.log("[Sender] Payload prepared (size: " + plaintextPayload.length + " bytes).");

            // Encrypt with AES-GCM
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            logger.log("[Sender] IV generated for AES-GCM (Base64): " + Base64.getEncoder().encodeToString(iv));

            byte[] encryptedPayload = CryptoUtils.encryptAESGCM(plaintextPayload, aesKey, iv);
            logger.log("[Sender] Payload encrypted (size: " + encryptedPayload.length + " bytes).");

            //Compose data to be signed: ephemeralPubKey + nonce + timestamp + iv + encryptedPayload
            byte[] ephemeralPubKeyBytes = ephemeralKeyPair.getPublic().getEncoded();

            ByteArrayOutputStream signStream = new ByteArrayOutputStream();
            DataOutputStream signDos = new DataOutputStream(signStream);

            signDos.writeInt(ephemeralPubKeyBytes.length);
            signDos.write(ephemeralPubKeyBytes);

            signDos.writeInt(nonce.length);
            signDos.write(nonce);

            signDos.writeLong(timestamp);

            signDos.writeInt(iv.length);
            signDos.write(iv);

            signDos.writeInt(encryptedPayload.length);
            signDos.write(encryptedPayload);

            byte[] dataToSign = signStream.toByteArray();

            logger.log("[Sender] Data to sign composed (size: " + dataToSign.length + " bytes).");

            //Sign the data using sender's private key
            byte[] signature = CryptoUtils.signData(dataToSign, KeyManager.senderKeyPair.getPrivate());
            logger.log("[Sender] Data signed (signature Base64): " + Base64.getEncoder().encodeToString(signature));

            // Send all data in order
            dos.writeInt(ephemeralPubKeyBytes.length);
            dos.write(ephemeralPubKeyBytes);

            dos.writeInt(nonce.length);
            dos.write(nonce);

            dos.writeLong(timestamp);

            dos.writeInt(iv.length);
            dos.write(iv);

            dos.writeInt(encryptedPayload.length);
            dos.write(encryptedPayload);

            dos.writeInt(signature.length);
            dos.write(signature);

            logger.log("[Sender] File sent with forward secrecy, AEAD, and signed metadata.");

        } catch (Exception e) {
            logger.log("[Sender] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
