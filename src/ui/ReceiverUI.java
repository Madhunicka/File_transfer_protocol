package ui;

import crypto.FileReceiver;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ReceiverUI {
    public static void createReceiverUI() {
        JFrame frame = new JFrame("Secure File Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        UILogger logger = new UILogger(logArea);

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
            String originalName = FileReceiver.receivedFileName;
            File tempFile = FileReceiver.getTempDecryptedFile();

            if (tempFile == null || !tempFile.exists()) {
                logger.log("[Receiver] No file to save.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Received File As");
            chooser.setSelectedFile(new File(originalName));

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File dest = chooser.getSelectedFile();
                try {
                    Files.move(tempFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.log("[Receiver] File saved to: " + dest.getAbsolutePath());
                    savePathField.setText(dest.getAbsolutePath());
                    saveButton.setEnabled(false);
                } catch (Exception ex) {
                    logger.log("[Receiver] Save Error: " + ex.getMessage());
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);


        FileReceiver.startReceiver(logger, () -> SwingUtilities.invokeLater(() -> {
            saveButton.setEnabled(true);
            savePathField.setText("");
        }));
    }
}
