// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.


public class Main {
    public static void main(String[] args) {
        // No need to call generateKeys(), KeyManager initializes keys automatically

        new Thread(ui.ReceiverUI::createReceiverUI).start();
        javax.swing.SwingUtilities.invokeLater(ui.SenderUI::createSenderUI);
    }
}
