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
        JTextArea logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        UILogger logger = new UILogger(logArea);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("File Path:"));
        inputPanel.add(filePathField);
        inputPanel.add(selectFileButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        selectFileButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                filePathField.setText(f.getAbsolutePath());
                logger.log("File selected: " + f.getAbsolutePath());
                FileSender.sendFile(f, logger);
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
