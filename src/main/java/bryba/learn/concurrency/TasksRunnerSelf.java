package bryba.learn.concurrency;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class TasksRunnerSelf
{
    static Semaphore semaphore = new Semaphore(1);
    static volatile Thread globalThreadHandle = null;

    public static void main(String[] args)
            throws InterruptedException
    {
        ScheduledExecutorService exec = newScheduledThreadPool(4, daemonThreadsNamed("test"));
        Runnable task = createTaskInterruptedToInterrupt();

        semaphore.acquire();
        ScheduledFuture<?> future = exec.scheduleWithFixedDelay(task, 0, 5000, TimeUnit.MILLISECONDS);
        if (semaphore.tryAcquire(5500, TimeUnit.MILLISECONDS)) {
            try {
                Thread.sleep(100);
                log("MAIN - interrupt");
                globalThreadHandle.interrupt();
//                exec.shutdownNow();
            }
            finally {
                semaphore.release();
            }
        }
        try {
            Object a = future.get();
            log("Future get OK");
        }
        catch (Throwable e) {
            log("Future get " + e);
            e.printStackTrace();
        }
        Thread.sleep(1000);
    }

    private static Runnable createTaskInterruptedSwallow()
    {
        return () -> {
            log("TASK - start");
            try {
                semaphore.release();
                log("TASK - sleeping");
                globalThreadHandle = Thread.currentThread();
                Thread.sleep(1000);
                log("TASK - wake up");
            }
            catch (InterruptedException e) {
                log("TASK - INTERRUPTED Swallow");
                e.printStackTrace();
            }
        };
    }

    private static Runnable createTaskInterruptedToRuntime()
    {
        return () -> {
            log("TASK - start");
            try {
                semaphore.release();
                log("TASK - sleeping");
                globalThreadHandle = Thread.currentThread();
                Thread.sleep(1000);
                log("TASK - wake up");
            }
            catch (InterruptedException e) {
                log("TASK - INTERRUPTED to Runtime");
                throw new RuntimeException(e);
            }
        };
    }

    private static Runnable createTaskInterruptedToInterrupt()
    {
        return () -> {
            log("TASK - start");
            try {
                semaphore.release();
                log("TASK - sleeping");
                globalThreadHandle = Thread.currentThread();
                Thread.sleep(1000);
                log("TASK - wake up");
            }
            catch (InterruptedException e) {
                log("TASK - INTERRUPTED to Interrupt before");
                Thread.currentThread().interrupt();
                log("TASK - INTERRUPTED to Interrupt after");
                try {
                    log("TASK - INTERRUPTED to Interrupt sleep again");
                    Thread.sleep(1000);
                }
                catch (InterruptedException ex) {
                    log("TASK - INTERRUPTED to Interrupt sleep again INTERRUPTED " + ex);
                }
            }
        };
    }

    private static void log(String msg)
    {
        System.out.println(Thread.currentThread().getName() + " - " + Thread.currentThread().isInterrupted() + " - " + msg);
    }

    private static ThreadFactory daemonThreadsNamed(String s)
    {
        return new ThreadFactory()
        {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, s + "=" + counter.incrementAndGet());
            }
        };
    }
}
