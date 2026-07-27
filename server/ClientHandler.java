import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String nickname;
    private boolean authenticated = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getNickname() {
        return nickname;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // (Login / Registrazione)
            String inputLine;
            while (!authenticated && (inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("/LOGIN ")) {
                    handleLogin(inputLine.substring(7));
                } else if (inputLine.startsWith("/REGISTER ")) {
                    handleRegister(inputLine.substring(10));
                }
            }

            if (!authenticated) return;

            //tutti gli utenti
            sendRegisteredUsersList();

            //norifica nuovo utente
            broadcastUserJoined(this.nickname);

            //  Ciclo ascolto messaggi
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("exit")) {
                    break;
                }

                if (inputLine.startsWith("/loadhistory ")) {
                    String targetUser = inputLine.substring(13).trim();
                    sendPrivateHistory(targetUser);
                } 
                else if (inputLine.startsWith("/whisper ") || inputLine.startsWith("/w ")) {
                    handlePrivateMessage(inputLine);
                }
            }
        } catch (IOException e) {
            System.err.println("[THREAD] Disconnesso: " + nickname);
        } finally {
            if (authenticated && nickname != null) {
                MainServer.activeClients.remove(nickname);
                broadcastUserLeft(this.nickname);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLogin(String data) {
        String[] parts = data.split(" ", 2);
        if (parts.length < 2) {
            out.println("[AUTH_FAIL] Formato errato.");
            return;
        }

        String user = parts[0];
        String pass = parts[1];

        if (MainServer.activeClients.containsKey(user)) {
            out.println("[AUTH_FAIL] Utente già connesso su un altro dispositivo!");
            return;
        }

        if (DatabaseManager.checkLogin(user, pass)) {
            this.nickname = user;
            this.authenticated = true;
            MainServer.activeClients.put(this.nickname, this);
            out.println("[AUTH_SUCCESS] " + this.nickname);
        } else {
            out.println("[AUTH_FAIL] Username o Password errati!");
        }
    }

    private void handleRegister(String data) {
        String[] parts = data.split(" ", 2);
        if (parts.length < 2) {
            out.println("[AUTH_FAIL] Formato errato.");
            return;
        }

        String user = parts[0];
        String pass = parts[1];

        if (DatabaseManager.registerUser(user, pass)) {
            out.println("[REG_SUCCESS] Registrazione completata! Ora puoi accedere.");
        } else {
            out.println("[AUTH_FAIL] Username già in uso. Scegline un altro.");
        }
    }

    private void sendRegisteredUsersList() {
        List<String> allUsers = DatabaseManager.getAllUsers();
        for (String u : allUsers) {
            if (!u.equalsIgnoreCase(this.nickname)) {
                out.println("[SYSTEM_REGISTERED_USER] " + u);
            }
        }
        
        for (String onlineUser : MainServer.activeClients.keySet()) {
            if (!onlineUser.equalsIgnoreCase(this.nickname)) {
                out.println("[SYSTEM_USER_JOINED] " + onlineUser);
            }
        }
    }

    private void sendPrivateHistory(String targetUser) {
        out.println("--- INIZIO STORICO " + targetUser + " ---");
        List<String> history = DatabaseManager.getPrivateHistory(this.nickname, targetUser);
        for (String line : history) {
            out.println("[HIST " + targetUser + "] " + line);
        }
        out.println("--- FINE STORICO " + targetUser + " ---");
    }

    private void handlePrivateMessage(String rawMessage) {
        String[] parts = rawMessage.split(" ", 3);
        if (parts.length < 3) return;

        String targetUser = parts[1].trim();
        String privateMsg = parts[2].trim();
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // salva nel database
        DatabaseManager.savePrivateMessage(this.nickname, targetUser, privateMsg, timestamp);

        
        for (Map.Entry<String, ClientHandler> entry : MainServer.activeClients.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(targetUser)) {
                entry.getValue().sendMessage("[" + timestamp + "] [PRIVATO da " + this.nickname + "]: " + privateMsg);
                break;
            }
        }
    }

    private void broadcastUserJoined(String user) {
        for (ClientHandler client : MainServer.activeClients.values()) {
            if (!client.getNickname().equalsIgnoreCase(user)) {
                client.sendMessage("[SYSTEM_USER_JOINED] " + user);
            }
        }
    }

    private void broadcastUserLeft(String user) {
        for (ClientHandler client : MainServer.activeClients.values()) {
            client.sendMessage("[SYSTEM_USER_LEFT] " + user);
        }
    }
}