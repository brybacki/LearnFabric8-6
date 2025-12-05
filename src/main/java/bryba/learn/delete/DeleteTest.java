package bryba.learn.delete;

import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.LifecycleHandlerBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.List;

/**
 * Run together with InformerTest to see pod deletion events.
 */
public class DeleteTest
{
    /*
    ❯ kind create cluster
     */
    public static void main(String[] args)
    {
        deleteTest();
    }

    public static void deleteTest()
    {
        Config c = Config.autoConfigure("kind-replica");
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            // Create namespace
            System.out.println("Creating namespace 'delete-test'...");
            client.namespaces().createOrReplace(
                    new NamespaceBuilder()
                            .withMetadata(new ObjectMetaBuilder()
                                    .withName("delete-test")
                                    .build())
                            .build());
            System.out.println("Namespace 'delete-test' created.");

            // Create pod with preStop hook that sleeps for 60 seconds
            System.out.println("Creating pod with 60-second preStop hook...");
            Pod pod = new PodBuilder()
                    .withMetadata(new ObjectMetaBuilder()
                            .withName("slow-termination-pod")
                            .withNamespace("delete-test")
                            .build())
                    .withSpec(new PodSpecBuilder()
                            .addNewContainer()
                            .withName("nginx")
                            .withImage("nginx:latest")
                            .withLifecycle(prestopHook())
                            .endContainer()
                            .build())
                    .build();

            client.pods().inNamespace("delete-test").createOrReplace(pod);
            System.out.println("Pod 'slow-termination-pod' created.");

            // Wait for pod to be running
            System.out.println("Waiting for pod to be ready...");
            client.pods().inNamespace("delete-test").withName("slow-termination-pod")
                    .waitUntilReady(30, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("Pod is ready.");

            // Delete the pod and measure time
            System.out.println("Deleting pod (this will take ~60 seconds due to graceful termination)...");
            long startTime = System.currentTimeMillis();

            List<StatusDetails> deleteStatus = client.pods().inNamespace("delete-test")
                    .withName("slow-termination-pod")
                    .delete();

            long deleteReturnTime = System.currentTimeMillis();
            long deleteCallDuration = (deleteReturnTime - startTime) / 1000;

            System.out.println("Delete operation returned immediately: " + deleteStatus);
            System.out.println("Delete call took: " + deleteCallDuration + " seconds");

            // Poll to check when the pod actually disappears
            System.out.println("Polling to check when pod actually disappears...");
            Pod podCheck;
            int pollCount = 0;
            do {
                Thread.sleep(2000); // Check every 2 seconds
                podCheck = client.pods().inNamespace("delete-test")
                        .withName("slow-termination-pod")
                        .get();
                pollCount++;
                if (podCheck != null) {
                    System.out.println("Poll #" + pollCount + ": Pod still exists (status: " +
                            podCheck.getStatus().getPhase() + ")");
                }
            } while (podCheck != null);

            long endTime = System.currentTimeMillis();
            long totalDuration = (endTime - startTime) / 1000;

            System.out.println("Pod has disappeared!");
            System.out.println("Total time for pod to be deleted: " + totalDuration + " seconds");
            System.out.println("The preStop hook caused a graceful termination delay.");

            // Clean up namespace
            System.out.println("Cleaning up namespace...");
            client.namespaces().withName("delete-test").delete();
            System.out.println("Namespace deleted.");
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Lifecycle prestopHook()
    {
        return new LifecycleBuilder()
                .withPreStop(new LifecycleHandlerBuilder()
                        .withExec(new ExecActionBuilder()
                                .withCommand("sh", "-c", "echo 'PreStop hook: sleeping for 60 seconds...' && sleep 20")
                                .build())
                        .build())
                .build();
    }
}
