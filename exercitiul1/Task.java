import java.util.concurrent.ConcurrentHashMap;

public class Task implements Runnable {
    private final int id;
    private final long durationMs;
    private final Object monitor;
    private final ConcurrentHashMap<Integer, TaskStatus> statusMap;
    private volatile boolean interrupted = false;
    
    public Task(int id, long durationMs, Object monitor, ConcurrentHashMap<Integer, TaskStatus> statusMap) {
        this.id = id;
        this.durationMs = durationMs;
        this.monitor = monitor;
        this.statusMap = statusMap;
    }
    
    @Override
    public void run() {
        System.out.println("Task " + id + " started");
        statusMap.put(id, TaskStatus.RUNNING);
        
        synchronized (monitor) {
            try {
                Thread.sleep(durationMs);
                
                if (!interrupted) {
                    synchronized (this) {
                        statusMap.put(id, TaskStatus.COMPLETED);
                        System.out.println("Task " + id + " completed successfully");
                    }
                }
            } catch (InterruptedException e) {
                statusMap.put(id, TaskStatus.INTERRUPTED);
                System.out.println("Task " + id + " was interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void timeout() {
        interrupted = true;
        statusMap.put(id, TaskStatus.TIMED_OUT);
        System.out.println("Task " + id + " timed out");
    }
    
    public int getId() {
        return id;
    }
}
