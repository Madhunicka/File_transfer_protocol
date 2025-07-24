package ui;

import javax.swing.*;


public class UILogger {
    private final JTextArea textArea;

    public UILogger(JTextArea textArea) {
        this.textArea = textArea;
    }

    public void log(String message) {
        System.out.println(message);
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
