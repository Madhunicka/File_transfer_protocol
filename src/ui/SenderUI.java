package ui;

import crypto.FileSender;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SenderUI {

    public static void createSenderUI() {
        JFrame frame = new JFrame("Secure File Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTextField filePathField = new JTextField(20);
        filePathField.setEditable(false);

        JButton selectFileButton = new JButton("Select File");
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.setEnabled(false); // disabled until file selected

        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        UILogger logger = new UILogger(logArea);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("File Path:"));
        inputPanel.add(filePathField);
        inputPanel.add(selectFileButton);
        inputPanel.add(sendFileButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        final File[] selectedFile = new File[1]; // to store selected file

        selectFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                filePathField.setText(f.getAbsolutePath());
                logger.log("File selected: " + f.getAbsolutePath());
                selectedFile[0] = f;
                sendFileButton.setEnabled(true);
            }
        });

        sendFileButton.addActionListener(e -> {
            if (selectedFile[0] != null) {
                sendFileButton.setEnabled(false);
                selectFileButton.setEnabled(false);
                logger.log("Starting file send...");

                // Run sending in a background thread
                new Thread(() -> {
                    FileSender.sendFile(selectedFile[0], logger);
                    SwingUtilities.invokeLater(() -> {
                        logger.log("File send finished.");
                        sendFileButton.setEnabled(true);
                        selectFileButton.setEnabled(true);
                    });
                }).start();
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SenderUI::createSenderUI);
    }
}
