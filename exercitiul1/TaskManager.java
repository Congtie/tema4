import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private static final int N_THREADS = 5;
    private static final long T_MAX = 3000;
    private static final Object monitor = new Object();
    
    public static void main(String[] args) {
        ConcurrentHashMap<Integer, TaskStatus> statusMap = new ConcurrentHashMap<>();
        List<Thread> threads = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        
        WatchdogThread watchdog = new WatchdogThread(statusMap);
        watchdog.start();
        
        for (int i = 1; i <= N_THREADS; i++) {
            long duration = 1000 + (long)(Math.random() * 4000);
            Task task = new Task(i, duration, monitor, statusMap);
            tasks.add(task);
            
            Thread thread = new Thread(task, "Task-" + i);
            threads.add(thread);
            thread.start();
        }
        
        Thread timeoutMonitor = new Thread(() -> {
            try {
                Thread.sleep(T_MAX);
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    if (statusMap.get(task.getId()) == TaskStatus.RUNNING) {
                        task.timeout();
                        
                        Thread thread = threads.get(i);
                        thread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Timeout monitor interrupted");
            }
        });
        timeoutMonitor.start();
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Main thread interrupted while waiting for task completion");
            }
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        watchdog.stopWatchdog();
        
        System.out.println("\n=== Final Status Report ===");
        for (ConcurrentHashMap.Entry<Integer, TaskStatus> entry : statusMap.entrySet()) {
            System.out.println("Task " + entry.getKey() + ": " + entry.getValue());
        }
        
        System.out.println("Task Manager finished execution.");
    }
}
