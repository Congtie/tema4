import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String clientName;
    private boolean active;
    private boolean isAdmin;
    
    private static final ThreadLocal<Socket> currentSocket = new ThreadLocal<>();
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.active = true;
        this.isAdmin = false;
        currentSocket.set(socket);
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            writer.println("Enter your name:");
            clientName = reader.readLine();
            
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Anonymous_" + clientSocket.getPort();
            }
            
            if (ChatServer.getActiveClientsCount() == 1 || "admin".equalsIgnoreCase(clientName)) {
                isAdmin = true;
                writer.println("Welcome " + clientName + "! You have admin privileges.");
                writer.println("Commands: /quit (disconnect), /shutdown (admin only - shutdown server)");
            } else {
                writer.println("Welcome " + clientName + "!");
                writer.println("Commands: /quit (disconnect)");
            }
            
            ChatServer.broadcastMessage("*** " + clientName + " joined the chat ***", this);
            
            String message;
            while (active && (message = reader.readLine()) != null) {
                if (message.startsWith("/quit")) {
                    break;
                } else if (message.startsWith("/shutdown")) {
                    if (isAdmin) {
                        writer.println("Shutting down server...");
                        ChatServer.shutdown();
                        break;
                    } else {
                        writer.println("ACCESS DENIED: Only admin can shutdown the server.");
                    }
                } else if (!message.trim().isEmpty()) {
                    String formattedMessage = clientName + ": " + message;
                    ChatServer.broadcastMessage(formattedMessage, this);
                }
            }
            
        } catch (IOException e) {
            if (active) {
                System.err.println("Error handling client " + clientName + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }
    
    public void sendMessage(String message) {
        if (writer != null && active) {
            writer.println(message);
        }
    }
    
    public void disconnect() {
        active = false;
        
        if (clientName != null) {
            ChatServer.broadcastMessage("*** " + clientName + " left the chat ***", this);
        }
        
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
        
        ChatServer.removeClient(this);
        
        currentSocket.remove();
        
        System.out.println("Client " + clientName + " disconnected");
    }
    
    public boolean isActive() {
        return active;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public static Socket getCurrentSocket() {
        return currentSocket.get();
    }
}
