import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleConnectionPool {
    private final ConcurrentLinkedQueue<Connection> availableConnections;
    private final int maxConnections;
    private int currentConnections;
    private final String url;
    private final String username;
    private final String password;
    
    public SimpleConnectionPool(String url, String username, String password, int maxConnections) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
        this.availableConnections = new ConcurrentLinkedQueue<>();
        this.currentConnections = 0;
        
        initializePool();
    }
    
    private void initializePool() {
        for (int i = 0; i < maxConnections; i++) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                availableConnections.offer(conn);
                currentConnections++;
                System.out.println("Created connection " + (i + 1) + "/" + maxConnections);
            } catch (SQLException e) {
                System.err.println("Error creating connection: " + e.getMessage());
            }
        }
        System.out.println("Connection pool initialized with " + currentConnections + " connections");
    }
    
    public synchronized Connection getConnection() throws InterruptedException {
        while (availableConnections.isEmpty()) {
            System.out.println(Thread.currentThread().getName() + " waiting for connection...");
            wait();
        }
        
        Connection conn = availableConnections.poll();
        System.out.println(Thread.currentThread().getName() + " acquired connection");
        return conn;
    }
    
    public synchronized void releaseConnection(Connection connection) {
        if (connection != null) {
            availableConnections.offer(connection);
            System.out.println(Thread.currentThread().getName() + " released connection");
            notifyAll();
        }
    }
    
    public synchronized void closeAllConnections() {
        while (!availableConnections.isEmpty()) {
            Connection conn = availableConnections.poll();
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        System.out.println("All connections closed");
    }
    
    public int getAvailableConnectionsCount() {
        return availableConnections.size();
    }
}
