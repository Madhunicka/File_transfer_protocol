import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import javax.swing.*;
import java.awt.*;
import java.util.Base64;

public class Crypto {
    private static SecretKey symmetricKey;
    private static KeyPair senderKeyPair, receiverKeyPair;
    private static String receivedOriginalFileName = "";

    public static void main(String[] args) {
        generateKeys();
        new Thread(Crypto::createReceiverUI).start();
        SwingUtilities.invokeLater(Crypto::createSenderUI);
    }

    private static void generateKeys() {
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

    private static void createSenderUI() {
        JFrame frame = new JFrame("Secure File Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTextField filePathField = new JTextField(20);
        filePathField.setEditable(false);
        JButton selectFileButton = new JButton("Select File");
        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("File Path:"));
        inputPanel.add(filePathField);
        inputPanel.add(selectFileButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        selectFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                log(logArea, "File selected: " + filePathField.getText());
                sendFile(fileChooser.getSelectedFile(), logArea);
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void sendFile(File inputFile, JTextArea logArea) {
        try (Socket socket = new Socket("localhost", 6000);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            byte[] fileBytes = Files.readAllBytes(inputFile.toPath());
            log(logArea, "[Sender] Original file content: " + new String(fileBytes));
            log(logArea, "[Sender] Original file (Base64): " + Base64.getEncoder().encodeToString(fileBytes));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivSpec);
            byte[] encryptedFile = cipher.doFinal(fileBytes);
            log(logArea, "[Sender] IV: " + Base64.getEncoder().encodeToString(iv));
            log(logArea, "[Sender] Encrypted File: " + Base64.getEncoder().encodeToString(encryptedFile));

            Cipher keyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            keyCipher.init(Cipher.ENCRYPT_MODE, receiverKeyPair.getPublic());
            byte[] encryptedSymmetricKey = keyCipher.doFinal(symmetricKey.getEncoded());
            log(logArea, "[Sender] Encrypted AES Key: " + Base64.getEncoder().encodeToString(encryptedSymmetricKey));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileHash = digest.digest(fileBytes);
            log(logArea, "[Sender] SHA-256 Hash (Base64): " + Base64.getEncoder().encodeToString(fileHash));
            log(logArea, "[Sender] SHA-256 Hash (Hex): " + bytesToHex(fileHash));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(senderKeyPair.getPrivate());
            signature.update(fileHash);
            byte[] digitalSignature = signature.sign();
            log(logArea, "[Sender] Digital Signature: " + Base64.getEncoder().encodeToString(digitalSignature));

            dos.writeUTF(inputFile.getName());
            dos.writeInt(iv.length); dos.write(iv);
            dos.writeInt(encryptedFile.length); dos.write(encryptedFile);
            dos.writeInt(encryptedSymmetricKey.length); dos.write(encryptedSymmetricKey);
            dos.writeInt(digitalSignature.length); dos.write(digitalSignature);

            log(logArea, "[Sender] File sent successfully.");

        } catch (Exception e) {
            log(logArea, "[Sender] Error: " + e.getMessage());
        }
    }

    private static void createReceiverUI() {
        JFrame frame = new JFrame("Secure File Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JButton saveButton = new JButton("Save Received File");
        saveButton.setEnabled(false);
        JTextField savePathField = new JTextField(30);
        savePathField.setEditable(false);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Save Path:"));
        inputPanel.add(savePathField);
        inputPanel.add(saveButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Received File As");
            fileChooser.setSelectedFile(new File(receivedOriginalFileName)); // Suggest original name

            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    Files.move(new File("temp_received.decrypted").toPath(),
                            selectedFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log(logArea, "[Receiver] File saved to: " + selectedFile.getAbsolutePath());
                    savePathField.setText(selectedFile.getAbsolutePath());
                    saveButton.setEnabled(false);
                } catch (Exception ex) {
                    log(logArea, "[Receiver] Save Error: " + ex.getMessage());
                }
            }
        });


        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> startReceiver(logArea, saveButton, savePathField)).start();
    }

    private static void startReceiver(JTextArea logArea, JButton saveButton, JTextField savePathField) {
        try (ServerSocket serverSocket = new ServerSocket(6000)) {
            log(logArea, "[Receiver] Listening on port 6000...");
            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    String fileName = dis.readUTF();
                    receivedOriginalFileName = fileName;
                    log(logArea, "[Receiver] File name: " + fileName);

                    byte[] iv = new byte[dis.readInt()]; dis.readFully(iv);
                    log(logArea, "[Receiver] IV: " + Base64.getEncoder().encodeToString(iv));

                    byte[] encryptedFile = new byte[dis.readInt()]; dis.readFully(encryptedFile);
                    log(logArea, "[Receiver] Encrypted File: " + Base64.getEncoder().encodeToString(encryptedFile));

                    byte[] encryptedKey = new byte[dis.readInt()]; dis.readFully(encryptedKey);
                    log(logArea, "[Receiver] Encrypted AES Key: " + Base64.getEncoder().encodeToString(encryptedKey));

                    byte[] signature = new byte[dis.readInt()]; dis.readFully(signature);
                    log(logArea, "[Receiver] Digital Signature: " + Base64.getEncoder().encodeToString(signature));

                    Cipher keyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    keyCipher.init(Cipher.DECRYPT_MODE, receiverKeyPair.getPrivate());
                    byte[] aesBytes = keyCipher.doFinal(encryptedKey);
                    SecretKey decryptedKey = new SecretKeySpec(aesBytes, "AES");
                    log(logArea, "[Receiver] Decrypted AES Key: " + Base64.getEncoder().encodeToString(decryptedKey.getEncoded()));

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, decryptedKey, new IvParameterSpec(iv));
                    byte[] decryptedFile = cipher.doFinal(encryptedFile);
                    log(logArea, "[Receiver] Decrypted File: " + new String(decryptedFile));
                    log(logArea, "[Receiver] Decrypted File (Base64): " + Base64.getEncoder().encodeToString(decryptedFile));

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(decryptedFile);
                    log(logArea, "[Receiver] SHA-256 Hash (Base64): " + Base64.getEncoder().encodeToString(hash));
                    log(logArea, "[Receiver] SHA-256 Hash (Hex): " + bytesToHex(hash));

                    Signature sig = Signature.getInstance("SHA256withRSA");
                    sig.initVerify(senderKeyPair.getPublic());
                    sig.update(hash);

                    if (sig.verify(signature)) {
                        log(logArea, "[Receiver] Signature verified.");
                        Files.write(new File("temp_received.decrypted").toPath(), decryptedFile);
                        String suggestedPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + fileName;
                        savePathField.setText(suggestedPath);
                        saveButton.setEnabled(true);
                    } else {
                        log(logArea, "[Receiver] Signature verification failed.");
                    }
                }
            }
        } catch (Exception e) {
            log(logArea, "[Receiver] Error: " + e.getMessage());
        }
    }

    private static void log(JTextArea logArea, String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
        System.out.println("[DEBUG] " + msg);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
