import java.util.concurrent.ConcurrentHashMap;

public class WatchdogThread extends Thread {
    private final ConcurrentHashMap<Integer, TaskStatus> statusMap;
    private volatile boolean running = true;
    
    public WatchdogThread(ConcurrentHashMap<Integer, TaskStatus> statusMap) {
        this.statusMap = statusMap;
        this.setDaemon(true);
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(500);
                displayStatus();
            } catch (InterruptedException e) {
                System.out.println("Watchdog interrupted");
                break;
            }
        }
    }
    
    private void displayStatus() {
        System.out.println("\n=== Task Status Report ===");
        System.out.printf("%-10s %-15s%n", "Task ID", "Status");
        System.out.println("-------------------------");
        
        for (ConcurrentHashMap.Entry<Integer, TaskStatus> entry : statusMap.entrySet()) {
            System.out.printf("%-10d %-15s%n", entry.getKey(), entry.getValue());
        }
        System.out.println("=========================\n");
    }
    
    public void stopWatchdog() {
        running = false;
        this.interrupt();
    }
}
