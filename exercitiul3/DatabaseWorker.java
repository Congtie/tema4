import java.sql.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseWorker implements Runnable {
    private final SimpleConnectionPool connectionPool;
    private final int workerId;
    private final AtomicInteger totalInserts;
    private final Random random;
    
    public DatabaseWorker(SimpleConnectionPool connectionPool, int workerId, AtomicInteger totalInserts) {
        this.connectionPool = connectionPool;
        this.workerId = workerId;
        this.totalInserts = totalInserts;
        this.random = new Random();
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            Connection conn = null;
            try {
                conn = connectionPool.getConnection();
                
                insertLogMessage(conn, "Worker-" + workerId + " message " + (i + 1));
                
                int waitTime = 100 + random.nextInt(401);
                Thread.sleep(waitTime);
                
                totalInserts.incrementAndGet();
                
            } catch (InterruptedException e) {
                System.err.println("Worker " + workerId + " interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (SQLException e) {
                System.err.println("Database error in worker " + workerId + ": " + e.getMessage());
            } finally {
                if (conn != null) {
                    connectionPool.releaseConnection(conn);
                }
            }
        }
        System.out.println("Worker " + workerId + " completed");
    }
    
    private void insertLogMessage(Connection conn, String message) throws SQLException {
        String sql = "INSERT INTO Log (timestamp, message, worker_id) VALUES (CURRENT_TIMESTAMP, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.setInt(2, workerId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Worker " + workerId + " inserted: " + message);
            }
        }
    }
}
