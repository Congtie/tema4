import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;
    
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
    
    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            
            System.out.println("Connected to chat server at " + SERVER_HOST + ":" + SERVER_PORT);
            
            Thread messageListener = new Thread(this::listenForMessages);
            messageListener.setDaemon(true);
            messageListener.start();
            
            Thread inputHandler = new Thread(this::handleUserInput);
            inputHandler.start();
            
            inputHandler.join();
            
        } catch (ConnectException e) {
            System.err.println("Could not connect to server. Is the server running?");
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Client interrupted");
        } finally {
            disconnect();
        }
    }
    
    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                System.out.println(message);
                
                if (message.contains("Server is shutting down")) {
                    System.out.println("Server is shutting down. Disconnecting...");
                    connected = false;
                    break;
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Error receiving messages: " + e.getMessage());
            }
        }
    }
    
    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        
        try {
            while (connected) {
                String input = scanner.nextLine();
                
                if (input == null) {
                    break;
                }
                
                writer.println(input);
                
                if (input.startsWith("/quit")) {
                    System.out.println("Disconnecting...");
                    break;
                }
                
                if (input.startsWith("/shutdown")) {
                    System.out.println("Shutdown command sent...");
                }
            }
        } catch (Exception e) {
            if (connected) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        } finally {
            scanner.close();
        }
    }
    
    private void disconnect() {
        connected = false;
        
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        
        System.out.println("Disconnected from server.");
    }
}
