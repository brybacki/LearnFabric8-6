package bryba.learn.concurrency;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class ScheduledExec
{
    private final ScheduledExecutorService shutdownHandler = newSingleThreadScheduledExecutor(threadsNamed("shutdown-handler"));
    private volatile boolean isTerminating;
    private ScheduledFuture<Object> scheduledShutdownTask;

    private ThreadFactory threadsNamed(String s)
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

    public static void main(String[] args)
            throws InterruptedException
    {
        ScheduledExec exec = new ScheduledExec();
        exec.start();
    }

    public void start()
            throws InterruptedException
    {

        schedueldShutdown();
        Thread.sleep(3000);
        System.out.println("3 seconds passed");

        cancelShutdown();
        Thread.sleep(3000);
        System.out.println("Another 3 seconds passed");

        System.exit(0);
    }

    private void cancelShutdown()
    {
        isTerminating = false;
        scheduledShutdownTask.cancel(true);
    }

    public void schedueldShutdown()
    {
        isTerminating = true;
        scheduledShutdownTask = shutdownHandler.schedule(this::shutdown, 5000, TimeUnit.MILLISECONDS);
    }

    private Object shutdown()
    {
        if (isTerminating) {
            System.out.println("SHUTDOWN Called");
        }
        else {
            System.out.println("not terminating");
        }
        return null;
    }
}
