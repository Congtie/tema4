import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DeadlockDemo {
    private static final List<String> bufferA = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> bufferB = Collections.synchronizedList(new ArrayList<>());
    
    private static final Lock lockA = new ReentrantLock();
    private static final Lock lockB = new ReentrantLock();
    
    public static void main(String[] args) {
        System.out.println("=== Demonstrating Deadlock ===");
        demonstrateDeadlock();
        
        System.out.println("\n=== Resolving Deadlock ===");
        resolveDeadlock();
    }    
    public static void demonstrateDeadlock() {
        Thread p1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {                synchronized (bufferA) {
                    System.out.println("P1: Locked BufferA");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    synchronized (bufferB) {
                        System.out.println("P1: Locked BufferB");
                        bufferA.add("P1-Item-" + i);
                        bufferB.add("P1-Item-" + i);
                        System.out.println("P1: Added item " + i);
                    }
                    System.out.println("P1: Released BufferB");
                }
                System.out.println("P1: Released BufferA");
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Producer-P1");
        
        Thread p2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                synchronized (bufferB) {
                    System.out.println("P2: Locked BufferB");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    synchronized (bufferA) {
                        System.out.println("P2: Locked BufferA");
                        bufferA.add("P2-Item-" + i);
                        bufferB.add("P2-Item-" + i);
                        System.out.println("P2: Added item " + i);
                    }
                    System.out.println("P2: Released BufferA");
                }
                System.out.println("P2: Released BufferB");
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Producer-P2");
        
        Thread c1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                synchronized (bufferA) {
                    synchronized (bufferB) {
                        if (!bufferA.isEmpty() && !bufferB.isEmpty()) {
                            String itemA = bufferA.remove(0);
                            String itemB = bufferB.remove(0);
                            System.out.println("C1: Consumed " + itemA + " and " + itemB);
                        }
                    }
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Consumer-C1");
        
        p1.start();
        p2.start();
        c1.start();
        
        try {
            p1.join(3000);
            p2.join(3000);
            c1.join(3000);
            
            if (p1.isAlive() || p2.isAlive() || c1.isAlive()) {
                System.out.println("DEADLOCK DETECTED! Some threads are still alive after timeout.");
                p1.interrupt();
                p2.interrupt();
                c1.interrupt();
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        bufferA.clear();
        bufferB.clear();
    }
    
    public static void resolveDeadlock() {
        Thread p1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    if (lockA.tryLock(1000, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("P1: Acquired LockA");
                            if (lockB.tryLock(1000, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("P1: Acquired LockB");
                                    bufferA.add("P1-Safe-Item-" + i);
                                    bufferB.add("P1-Safe-Item-" + i);
                                    System.out.println("P1: Safely added item " + i);
                                } finally {
                                    lockB.unlock();
                                    System.out.println("P1: Released LockB");
                                }
                            } else {
                                System.out.println("P1: Could not acquire LockB, backing off");
                            }
                        } finally {
                            lockA.unlock();
                            System.out.println("P1: Released LockA");
                        }
                    } else {
                        System.out.println("P1: Could not acquire LockA, backing off");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Safe-Producer-P1");
        
        Thread p2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    if (lockA.tryLock(1000, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("P2: Acquired LockA");
                            if (lockB.tryLock(1000, TimeUnit.MILLISECONDS)) {
                                try {
                                    System.out.println("P2: Acquired LockB");
                                    bufferA.add("P2-Safe-Item-" + i);
                                    bufferB.add("P2-Safe-Item-" + i);
                                    System.out.println("P2: Safely added item " + i);
                                } finally {
                                    lockB.unlock();
                                    System.out.println("P2: Released LockB");
                                }
                            } else {
                                System.out.println("P2: Could not acquire LockB, backing off");
                            }
                        } finally {
                            lockA.unlock();
                            System.out.println("P2: Released LockA");
                        }
                    } else {
                        System.out.println("P2: Could not acquire LockA, backing off");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Safe-Producer-P2");
        
        Thread c1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    if (lockA.tryLock(1000, TimeUnit.MILLISECONDS)) {
                        try {
                            if (lockB.tryLock(1000, TimeUnit.MILLISECONDS)) {
                                try {
                                    if (!bufferA.isEmpty() && !bufferB.isEmpty()) {
                                        String itemA = bufferA.remove(0);
                                        String itemB = bufferB.remove(0);
                                        System.out.println("C1: Safely consumed " + itemA + " and " + itemB);
                                    }
                                } finally {
                                    lockB.unlock();
                                }
                            }
                        } finally {
                            lockA.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Safe-Consumer-C1");
        
        p1.start();
        p2.start();
        c1.start();
        
        try {
            p1.join();
            p2.join();
            c1.join();
            System.out.println("All threads completed successfully - NO DEADLOCK!");
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        System.out.println("Final buffer sizes - A: " + bufferA.size() + ", B: " + bufferB.size());
    }
}
