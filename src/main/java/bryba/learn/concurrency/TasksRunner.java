package bryba.learn.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class TasksRunner
{
    static Semaphore semaphore = new Semaphore(1);

    public static void main(String[] args)
            throws InterruptedException
    {
        ScheduledExecutorService exec = newScheduledThreadPool(4, daemonThreadsNamed("test"));
        Runnable task = createTaskInterruptedSwallow();

        semaphore.acquire();
        ScheduledFuture<?> future = exec.scheduleWithFixedDelay(task, 0, 5000, TimeUnit.MILLISECONDS);
        if (semaphore.tryAcquire(5500, TimeUnit.MILLISECONDS)) {
            try {
                Thread.sleep(100);
                System.out.println("MAIN - cancel future");
                future.cancel(true);
            }
            finally {
                semaphore.release();
            }
        }
        try {
            Object a = future.get();
            System.out.println("Future get OK");
        }
        catch (Throwable e) {
            System.out.println("Future get " + e);
        }
        Thread.sleep(1000);
    }

    private static Runnable createTaskInterruptedSwallow()
    {
        return () -> {
            System.out.println("TASK - start");
            try {
                semaphore.release();
                System.out.println("TASK - sleeping");
                Thread.sleep(1000);
                System.out.println("TASK - wake up");
            }
            catch (InterruptedException e) {
                System.out.println("TASK - INTERRUPTED");
            }
        };
    }

    private static Runnable createTaskInterruptedToRuntime()
    {
        return () -> {
            System.out.println("TASK - start");
            try {
                semaphore.release();
                System.out.println("TASK - sleeping");
                Thread.sleep(1000);
                System.out.println("TASK - wake up");
            }
            catch (InterruptedException e) {
                System.out.println("TASK - INTERRUPTED");
                throw new RuntimeException(e);
            }
        };
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
