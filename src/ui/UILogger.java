package ui;

import javax.swing.*;

public class UILogger {
    private JTextArea logArea;
    public UILogger(JTextArea area) { logArea = area; }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
        System.out.println("[DEBUG] " + msg);
    }
}
