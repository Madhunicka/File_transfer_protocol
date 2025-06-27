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

        JTextArea logArea = new JTextArea(15, 60);
        logArea.setEditable(false);
        UILogger logger = new UILogger(logArea);

        JButton saveButton = new JButton("Save Received File");
        saveButton.setEnabled(false);

        JTextField savePathField = new JTextField(40);
        savePathField.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Saved File Path:"));
        topPanel.add(savePathField);
        topPanel.add(saveButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        saveButton.addActionListener(e -> {
            File tempFile = FileReceiver.getLastReceivedFile();
            String originalName = FileReceiver.getLastReceivedFileName();

            if (tempFile == null || !tempFile.exists()) {
                logger.log("[ReceiverUI] No file to save.");
                saveButton.setEnabled(false);
                savePathField.setText("");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Received File As");
            chooser.setSelectedFile(new File(originalName));

            int result = chooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File dest = chooser.getSelectedFile();
                try {
                    Files.move(tempFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.log("[ReceiverUI] File saved to: " + dest.getAbsolutePath());
                    savePathField.setText(dest.getAbsolutePath());
                    saveButton.setEnabled(false);
                    FileReceiver.clearLastReceivedFile();
                } catch (Exception ex) {
                    logger.log("[ReceiverUI] Save error: " + ex.getMessage());
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> FileReceiver.startReceiver(logger, () ->
                SwingUtilities.invokeLater(() -> saveButton.setEnabled(true))
        )).start();
    }
}
