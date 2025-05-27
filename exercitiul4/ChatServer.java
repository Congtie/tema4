import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final int PORT = 8888;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static volatile boolean serverRunning = true;
    private static ServerSocket serverSocket;
    
    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Chat Server started on port " + PORT);
            System.out.println("Waiting for clients to connect...");
            
            while (serverRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                    
                } catch (SocketException e) {
                    if (serverRunning) {
                        System.err.println("Socket error: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
    
    public static synchronized void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("Broadcasting: " + message);
        for (ClientHandler client : clients) {
            if (client != sender && client.isActive()) {
                client.sendMessage(message);
            }
        }
    }
    
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Active clients: " + clients.size());
    }
    
    public static synchronized void shutdown() {
        System.out.println("Server shutdown initiated by admin...");
        serverRunning = false;
        
        for (ClientHandler client : clients) {
            client.sendMessage("SERVER: Server is shutting down. Goodbye!");
            client.disconnect();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket during shutdown: " + e.getMessage());
        }
        
        System.out.println("Server shutdown complete.");
    }
    
    public static int getActiveClientsCount() {
        return clients.size();
    }
}
