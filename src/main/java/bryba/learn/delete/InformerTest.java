package bryba.learn.delete;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import java.util.concurrent.TimeUnit;

/**
 * kind create cluster --name kind-replica
 */
public class InformerTest
{
    public static void main(String[] args)
            throws InterruptedException
    {
        informer();
    }

    public static void informer()
            throws InterruptedException
    {
        Config c = Config.autoConfigure("kind-replica");

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            System.out.println("INSIDE informer");

            SharedIndexInformer<Pod> informer = client.pods()
                    .inNamespace("delete-test")
//                    .withLabel("inform", "true")
                    .runnableInformer(60 * 1000L);

//            informer.stopped().whenComplete((v, t) -> {
//                if (t != null) {
//                    System.out.println("Exception occurred, caught: {}", t.getMessage());
//                }
//            });
            informer.addEventHandler(new ResourceEventHandler<>()
            {
                @Override
                public void onAdd(Pod pod)
                {
                    logPodDetails("✨ ADD", pod);
                }

                @Override
                public void onUpdate(Pod pod, Pod newPod)
                {
                    logPodDetails("🔄 UPDATE", newPod);
                }

                @Override
                public void onDelete(Pod pod, boolean b)
                {
                    logPodDetails("💀 DELETE", pod);
                }
            });

//            Executors.newSingleThreadExecutor().submit(() -> {
//                Thread.currentThread().setName("HAS_SYNCED_THREAD");
//                try {
//                    for (;;) {
//                        System.out.println("podInformer.hasSynced() : " + informer.hasSynced());
//                        Thread.sleep(5 * 1000L);
//                    }
//                } catch (InterruptedException inEx) {
//                    Thread.currentThread().interrupt();
//                    System.out.println("HAS_SYNCED_THREAD interrupted: {}");
//                }
//            });

            informer.start();

            System.out.println("Starting all registered informers");
            TimeUnit.MINUTES.sleep(10);
        }
    }

    static void logPodDetails(String eventType, Pod pod)
    {
        System.out.println("════════════════════════════════════════════════════════════════");
        String ns = pod.getMetadata().getNamespace();
        String name = pod.getMetadata().getName();
        System.out.println(eventType + " " + ns + "/" + name);

        if (pod.getMetadata().getDeletionTimestamp() != null) {
            System.out.println("Pod " + pod.getMetadata().getName() + " is terminating - DeletionTimestamp: " + pod.getMetadata().getDeletionTimestamp());
        }

        // Display pod conditions
        if (pod.getStatus() != null && pod.getStatus().getConditions() != null) {
            System.out.println("Pod " + pod.getMetadata().getName() + " conditions:");
            pod.getStatus().getConditions().forEach(condition -> {
                String statusEmoji = "True".equals(condition.getStatus()) ? "✅" : "❌";
                System.out.println("  - " + condition.getType() + ": " + statusEmoji + " " + condition.getStatus() +
                                 " (Reason: " + condition.getReason() + ", Message: " + condition.getMessage() + ")");
            });
        }
    }
}
