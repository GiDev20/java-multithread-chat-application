import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientGUI extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    
    private static final Color COLOR_SIDEBAR_BG = new Color(255, 255, 255);
    private static final Color COLOR_SIDEBAR_HEADER = new Color(240, 242, 245);
    private static final Color COLOR_SIDEBAR_SELECTION = new Color(235, 237, 240);
    private static final Color COLOR_CHAT_BG = new Color(238, 234, 226); 
    private static final Color COLOR_HEADER_BG = new Color(240, 242, 245);
    private static final Color COLOR_TEXT_AREA_BG = new Color(255, 255, 255);
    private static final Color COLOR_INPUT_BG = new Color(255, 255, 255);
    private static final Color COLOR_ACCENT = new Color(0, 128, 105); 
    private static final Color COLOR_TEXT_PRIMARY = new Color(17, 27, 33);
    private static final Color COLOR_TEXT_SECONDARY = new Color(102, 119, 129);
    private static final Color COLOR_BORDER = new Color(224, 224, 224);

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private JLabel chatHeaderLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;
    private String currentRecipient = null;

    public ClientGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (!connectAndAuthenticate()) {
            System.exit(0);
        }

        setTitle("Chat Client - " + this.nickname);
        setSize(900, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar Contatti
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(COLOR_SIDEBAR_BG);
        leftPanel.setPreferredSize(new Dimension(260, 0));

        // Header Sidebar
        JPanel sidebarHeaderPanel = new JPanel(new BorderLayout());
        sidebarHeaderPanel.setBackground(COLOR_SIDEBAR_HEADER);
        sidebarHeaderPanel.setPreferredSize(new Dimension(0, 50));
        
        JLabel sidebarHeader = new JLabel("  Contatti", SwingConstants.LEFT);
        sidebarHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sidebarHeader.setForeground(COLOR_TEXT_PRIMARY);
        sidebarHeaderPanel.add(sidebarHeader, BorderLayout.CENTER);
        leftPanel.add(sidebarHeaderPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(COLOR_SIDEBAR_BG);
        userList.setForeground(COLOR_TEXT_PRIMARY);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
		// renderer
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(10, 15, 10, 15));
                String text = value.toString();

                if (isSelected) {
                    label.setBackground(COLOR_SIDEBAR_SELECTION);
                    label.setForeground(COLOR_TEXT_PRIMARY);
                } else {
                    label.setBackground(COLOR_SIDEBAR_BG);
                    label.setForeground(text.contains("(Online)") ? COLOR_TEXT_PRIMARY : COLOR_TEXT_SECONDARY);
                }

                if (text.contains("[*]")) {
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                    label.setForeground(COLOR_ACCENT);
                } else {
                    label.setFont(label.getFont().deriveFont(Font.PLAIN));
                }

                return label;
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    String target = cleanUsername(selected);
                    
                    if (!target.equalsIgnoreCase(this.nickname) && !target.equals(currentRecipient)) {
                        currentRecipient = target;
                        chatHeaderLabel.setText("  " + currentRecipient);
                        removeUnreadNotification(target);

                        chatArea.setText("");
                        out.println("/loadhistory " + currentRecipient);
                    }
                }
            }
        });

        JScrollPane leftScrollPane = new JScrollPane(userList);
        leftScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // area messaggi
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(COLOR_CHAT_BG);

       
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER_BG);
        headerPanel.setPreferredSize(new Dimension(0, 50));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        
        chatHeaderLabel = new JLabel("  Seleziona un contatto per chattare", SwingConstants.LEFT);
        chatHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatHeaderLabel.setForeground(COLOR_TEXT_PRIMARY);
        headerPanel.add(chatHeaderLabel, BorderLayout.CENTER);
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(COLOR_TEXT_AREA_BG);
        chatArea.setForeground(COLOR_TEXT_PRIMARY);
        chatArea.setMargin(new Insets(15, 15, 15, 15));
        
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(COLOR_HEADER_BG);
        inputPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(COLOR_INPUT_BG);
        inputField.setForeground(COLOR_TEXT_PRIMARY);
        inputField.setCaretColor(COLOR_TEXT_PRIMARY);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        sendButton = new JButton("Invia");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(COLOR_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);

        ActionListener sendListener = e -> sendMessage();
        sendButton.addActionListener(sendListener);
        inputField.addActionListener(sendListener);

        new Thread(new IncomingMessagesHandler()).start();
    }

    private boolean connectAndAuthenticate() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
                JTextField userField = new JTextField();
                JPasswordField passField = new JPasswordField();
                panel.add(new JLabel("Username:"));
                panel.add(userField);
                panel.add(new JLabel("Password:"));
                panel.add(passField);

                Object[] options = {"Accedi", "Registrati", "Annulla"};
                int result = JOptionPane.showOptionDialog(
                        null, panel, "Accesso al Server Chat",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                        null, options, options[0]
                );

                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                    return false;
                }

                String user = userField.getText().trim();
                String pass = new String(passField.getPassword()).trim();

                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Inserisci sia Username che Password!", "Errore", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                if (result == JOptionPane.YES_OPTION) {
                    out.println("/LOGIN " + user + " " + pass);
                    String response = in.readLine();
                    if (response != null && response.startsWith("[AUTH_SUCCESS] ")) {
                        this.nickname = response.substring(15).trim();
                        return true;
                    } else {
                        String errMsg = (response != null && response.startsWith("[AUTH_FAIL] ")) ? response.substring(12) : "Errore Sconosciuto";
                        JOptionPane.showMessageDialog(null, errMsg, "Errore Login", JOptionPane.ERROR_MESSAGE);
                    }
                } else if (result == JOptionPane.NO_OPTION) {
                    out.println("/REGISTER " + user + " " + pass);
                    String response = in.readLine();
                    if (response != null && response.startsWith("[REG_SUCCESS] ")) {
                        JOptionPane.showMessageDialog(null, response.substring(14), "Registrazione OK", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        String errMsg = (response != null && response.startsWith("[AUTH_FAIL] ")) ? response.substring(12) : "Errore Registrazione";
                        JOptionPane.showMessageDialog(null, errMsg, "Errore Registrazione", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Impossibile connettersi al Server!", "Errore Connessione", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && currentRecipient != null) {
            out.println("/whisper " + currentRecipient + " " + text);

            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String line = "[" + timestamp + "] " + nickname + ": " + text;

            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());

            inputField.setText("");
        } else if (currentRecipient == null) {
            JOptionPane.showMessageDialog(this, "Seleziona prima un contatto dalla lista a sinistra!");
        }
    }

    private String cleanUsername(String rawItem) {
        return rawItem.replace("(Online)", "").replace("[*]", "").trim();
    }

    private void addUnreadNotification(String sender) {
        for (int i = 0; i < listModel.size(); i++) {
            String item = listModel.getElementAt(i);
            if (cleanUsername(item).equalsIgnoreCase(sender)) {
                if (!item.contains("[*]")) {
                    listModel.set(i, item + " [*]");
                }
                break;
            }
        }
    }

    private void removeUnreadNotification(String targetUser) {
        for (int i = 0; i < listModel.size(); i++) {
            String item = listModel.getElementAt(i);
            if (cleanUsername(item).equalsIgnoreCase(targetUser)) {
                if (item.contains("[*]")) {
                    listModel.set(i, item.replace("[*]", "").trim());
                }
                break;
            }
        }
    }

    private class IncomingMessagesHandler implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    final String msg = serverMessage;
                    SwingUtilities.invokeLater(() -> processIncomingMessage(msg));
                }
            } catch (IOException e) {
                System.err.println("Connessione persa.");
            }
        }

        private void processIncomingMessage(String msg) {
            if (msg.startsWith("[SYSTEM_REGISTERED_USER] ")) {
                String user = msg.substring(25).trim();
                if (!user.equalsIgnoreCase(nickname)) {
                    updateUserStatus(user, false);
                }
                return;
            }

            if (msg.startsWith("[SYSTEM_USER_JOINED] ")) {
                String user = msg.substring(21).trim();
                if (!user.equalsIgnoreCase(nickname)) {
                    updateUserStatus(user, true);
                }
                return;
            }

            if (msg.startsWith("[SYSTEM_USER_LEFT] ")) {
                String user = msg.substring(19).trim();
                if (!user.equalsIgnoreCase(nickname)) {
                    updateUserStatus(user, false);
                }
                return;
            }

            if (msg.startsWith("--- INIZIO STORICO ")) {
                chatArea.setText("");
                return;
            }
            if (msg.startsWith("--- FINE STORICO ")) {
                return;
            }

            if (msg.startsWith("[HIST ")) {
                int endTagIndex = msg.indexOf("] ");
                if (endTagIndex != -1) {
                    String cleanLine = msg.substring(endTagIndex + 2);
                    chatArea.append(cleanLine + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }
                return;
            }

            if (msg.contains("[PRIVATO da ")) {
                try {
                    int startSender = msg.indexOf("[PRIVATO da ") + 12;
                    int endSender = msg.indexOf("]:", startSender);

                    String sender = msg.substring(startSender, endSender).trim();
                    String textContent = msg.substring(endSender + 2);
                    String timestamp = msg.substring(0, msg.indexOf("]") + 1);

                    String formattedLine = timestamp + " " + sender + ":" + textContent;

                    if (currentRecipient != null && sender.equalsIgnoreCase(currentRecipient)) {
                        chatArea.append(formattedLine + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    } else {
                        addUnreadNotification(sender);
                    }
                } catch (Exception e) {
                    System.err.println("Errore parsing messaggio: " + e.getMessage());
                }
            }
        }

        private void updateUserStatus(String targetUser, boolean isOnline) {
            String offlineName = targetUser;
            String onlineName = targetUser + " (Online)";

            for (int i = 0; i < listModel.size(); i++) {
                String currentItem = listModel.getElementAt(i);
                if (cleanUsername(currentItem).equalsIgnoreCase(targetUser)) {
                    boolean hasNotification = currentItem.contains("[*]");
                    String baseName = isOnline ? onlineName : offlineName;
                    listModel.set(i, hasNotification ? baseName + " [*]" : baseName);
                    return;
                }
            }

            listModel.addElement(isOnline ? onlineName : offlineName);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI client = new ClientGUI();
            client.setVisible(true);
        });
    }
}