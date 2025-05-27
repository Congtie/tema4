import java.sql.*;

public class PostgreSQLConnectionTest {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/testdb";
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "123";
    
    public static void main(String[] args) {
        System.out.println("=== PostgreSQL Connection Test ===");
          try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✓ PostgreSQL JDBC driver loaded successfully");
            
            System.out.println("Attempting to connect to: " + DB_URL);
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            if (conn != null) {
                System.out.println("✓ Database connection successful!");
                
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT version()");
                
                if (rs.next()) {
                    System.out.println("✓ PostgreSQL version: " + rs.getString(1));
                }
                
                rs.close();
                stmt.close();
                conn.close();
                
                System.out.println("✓ Connection closed successfully");
                System.out.println("\nPostgreSQL is ready! You can now run JDBCConcurrentApp.");
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL JDBC driver not found: " + e.getMessage());
            System.err.println("Make sure postgresql-42.7.3.jar is in the classpath");
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            System.err.println("\nPossible solutions:");
            System.err.println("1. Make sure PostgreSQL is running on localhost:5432");
            System.err.println("2. Create database 'testdb': psql -U postgres -c \"CREATE DATABASE testdb;\"");
            System.err.println("3. Check if username/password are correct (current: postgres/password)");
            System.err.println("4. Make sure PostgreSQL accepts local connections");
        } catch (Exception e) {
            System.err.println("✗ Unexpected error: " + e.getMessage());
        }
    }
}
