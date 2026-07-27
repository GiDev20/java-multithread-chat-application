# Java Multi-Client Chat Application

Applicazione di chat client-server sviluppata in Java con architettura Socket TCP, interfaccia grafica Java Swing e SQLITE.

---



- **Autenticazione Sicura:** Sistema di registrazione e login con cifratura delle password tramite hashing SHA-256.
- **Messaggistica Privata:** Chat diretta tra utenti registrati.
- **Interfaccia Grafica (GUI):** Layout moderno tramite Java Swing, con supporto a notifica di messaggi non letti.
- **Stato Utenti in Tempo Reale:** Rilevamento e aggiornamento automatico dello stato degli utenti (Online / Offline).
- **Persistenza dei Dati:** Salvataggio automatico su database SQLite.
- **Gestione Concorrente:** Architettura multi-thread sul server tramite `ClientHandler` per gestire più connessioni simultanee.

---

## 📁 Struttura del Progetto

```text
├── client/          # Codice sorgente dell'interfaccia utente (ClientGUI, ClientMain)
├── server/          # Logica di backend, gestione connessioni e database (MainServer, DatabaseManager, ClientHandler)
├── lib/             # Dipendenze e librerie esterne (Driver JDBC SQLite, SLF4J)
├── .gitignore       
└── README.md


```
##Avvio applicazione (windows)

Utilizzare lo script Batch fornito (run_server.bat)

LATO SERVER

1 - cmd prompt
2 - cd server
3 - run_server
 
 LATO CLIENT
 
1 - cd client
2 - javac *.java
3 - java ClientGUI
