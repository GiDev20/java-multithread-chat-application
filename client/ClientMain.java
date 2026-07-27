import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=== AVVIO CLIENT CHAT ===");

        try {
            
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("[CLIENT] Connesso al server su " + SERVER_HOST + ":" + SERVER_PORT);

            // i/o socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // input da tastiera
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

            // messaggi in arrivo
            Thread listenThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("[CLIENT] Connessione chiusa dal server.");
                }
            });
            listenThread.start();

          //invio messaggio al server
            String userInput;
            while ((userInput = keyboard.readLine()) != null) {
                out.println(userInput); 
                
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            
            socket.close();
            System.out.println("[CLIENT] Disconnesso.");

        } catch (IOException e) {
            System.err.println("[CLIENT] Errore di connessione: " + e.getMessage());
        }
    }
}