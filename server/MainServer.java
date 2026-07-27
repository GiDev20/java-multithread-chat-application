import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private static final int PORT = 8080;
    
    // mappa thread-safe
    public static Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        
        DatabaseManager.initDatabase();
        
        System.out.println("=== AVVIO SERVER MULTICHAT PERMANENTE ===");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] In ascolto sulla porta " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Nuova connessione da: " 
                                   + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Errore del server: " + e.getMessage());
        }
    }

    //broadcast message
    public static void broadcast(String message) {
        for (ClientHandler client : activeClients.values()) {
            client.sendMessage(message);
        }
    }
}