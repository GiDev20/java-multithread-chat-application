import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat_database.db";

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERRORE] Driver SQLite non trovato nel classpath!");
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + " username TEXT PRIMARY KEY,"
                + " password TEXT NOT NULL"
                + ");";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " sender TEXT NOT NULL,"
                + " receiver TEXT NOT NULL,"
                + " message TEXT NOT NULL,"
                + " timestamp TEXT NOT NULL"
                + ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createMessagesTable);
            System.out.println("[DATABASE] Inizializzato con successo!");
        } catch (SQLException e) {
            System.err.println("[DATABASE ERRORE] " + e.getMessage());
        }
    }

    //  password cifrata in SHA-256
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; 
        }
    }

    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // Utente già esistente
        }
    }

    public static boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERRORE] " + e.getMessage());
            return false;
        }
    }

    
    public static List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username ASC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERRORE] Impossibile recuperare utenti: " + e.getMessage());
        }
        return users;
    }

    public static void savePrivateMessage(String sender, String receiver, String message, String timestamp) {
        String sql = "INSERT INTO messages(sender, receiver, message, timestamp) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);
            pstmt.setString(4, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERRORE] Impossibile salvare il messaggio: " + e.getMessage());
        }
    }

    public static List<String> getPrivateHistory(String user1, String user2) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT sender, message, timestamp FROM messages "
                   + "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) "
                   + "ORDER BY id ASC";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user1);
            pstmt.setString(2, user2);
            pstmt.setString(3, user2);
            pstmt.setString(4, user1);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String msg = rs.getString("message");
                String time = rs.getString("timestamp");
                history.add("[" + time + "] " + sender + ": " + msg);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERRORE] Impossibile leggere lo storico: " + e.getMessage());
        }
        return history;
    }
}