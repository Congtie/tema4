import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JDBCConcurrentApp {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/testdb";
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "123";
    private static final int POOL_SIZE = 3;
    private static final int WORKER_COUNT = 5;
    
    public static void main(String[] args) {
        SimpleConnectionPool connectionPool = null;
        
        try {
            Class.forName("org.postgresql.Driver");
            
            connectionPool = new SimpleConnectionPool(DB_URL, DB_USERNAME, DB_PASSWORD, POOL_SIZE);
            
            createLogTable(connectionPool);
            
            createStoredProcedure(connectionPool);
            
            AtomicInteger totalInserts = new AtomicInteger(0);
            
            List<Thread> workers = new ArrayList<>();
            for (int i = 1; i <= WORKER_COUNT; i++) {
                DatabaseWorker worker = new DatabaseWorker(connectionPool, i, totalInserts);
                Thread thread = new Thread(worker, "Worker-" + i);
                workers.add(thread);
                thread.start();
            }
            
            for (Thread worker : workers) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    System.err.println("Main thread interrupted while waiting for workers");
                }
            }
            
            System.out.println("\n=== Execution Summary ===");
            System.out.println("Total inserts by workers: " + totalInserts.get());
            
            int recordCount = countLogRecords(connectionPool);
            System.out.println("Records in Log table: " + recordCount);
            
            callCleanupProcedure(connectionPool);
            
            int recordCountAfterCleanup = countLogRecords(connectionPool);
            System.out.println("Records after cleanup: " + recordCountAfterCleanup);
            
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found: " + e.getMessage());
            System.err.println("Please add postgresql-xx.x.x.jar to your classpath");
        } catch (Exception e) {
            System.err.println("Error in main application: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connectionPool != null) {
                connectionPool.closeAllConnections();
            }
        }
    }
    
    private static void createLogTable(SimpleConnectionPool pool) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS Log (
                    id SERIAL PRIMARY KEY,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    message VARCHAR(255) NOT NULL,
                    worker_id INTEGER NOT NULL
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
                System.out.println("Log table created/verified");
            }
        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());
        } finally {
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }
    
    private static void createStoredProcedure(SimpleConnectionPool pool) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            String createProcedureSQL = """
                CREATE OR REPLACE FUNCTION cleanup_old_logs()
                RETURNS INTEGER AS $$
                DECLARE
                    deleted_count INTEGER;
                BEGIN
                    DELETE FROM Log 
                    WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '1 hour');
                    
                    GET DIAGNOSTICS deleted_count = ROW_COUNT;
                    RETURN deleted_count;
                END;
                $$ LANGUAGE plpgsql;
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createProcedureSQL);
                System.out.println("Stored procedure created/updated");
            }
        } catch (Exception e) {
            System.err.println("Error creating stored procedure: " + e.getMessage());
        } finally {
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }
    
    private static int countLogRecords(SimpleConnectionPool pool) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            String countSQL = "SELECT COUNT(*) FROM Log";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSQL)) {
                
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error counting records: " + e.getMessage());
        } finally {
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
        return 0;
    }
    
    private static void callCleanupProcedure(SimpleConnectionPool pool) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            String callSQL = "{? = call cleanup_old_logs()}";
            
            try (CallableStatement cstmt = conn.prepareCall(callSQL)) {
                cstmt.registerOutParameter(1, Types.INTEGER);
                cstmt.execute();
                
                int deletedCount = cstmt.getInt(1);
                System.out.println("Cleanup procedure executed, deleted " + deletedCount + " old records");
            }
        } catch (Exception e) {
            System.err.println("Error calling cleanup procedure: " + e.getMessage());
        } finally {
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }
}
