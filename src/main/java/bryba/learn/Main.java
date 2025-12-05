package bryba.learn;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import software.amazon.awssdk.utils.ImmutableMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is garbage class for learning and testing kubernetes-client features.
 */
public class Main
{
    /*
    kind -
    TODO:
      - verify is observer is executed on status change! or maybe only on spec change
      - verify how the version syncVersion changes
      -

     */
    public static void main(String[] args)
            throws IOException, InterruptedException
    {
        System.out.println("Hello world!");
        ImmutableMap<String, Object> map = ImmutableMap.of("asd", null, "abc", null);
        System.out.println(map);
//        animal();
        // Create informer filtered by Label

//        informer();
    }

    static void info(String command, String ns, String name)
    {
        System.out.println(command + " " + ns + "/" + name);
    }

    public static void informer()
            throws InterruptedException
    {
        Config c = Config.autoConfigure("kind-kind");

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            System.out.println("INSIDE informer");

            SharedIndexInformer<GenericKubernetesResource> informer = client.genericKubernetesResources("jungle.example.com/v1", "Animal")
//                    .inNamespace("abc2")
                    .withLabel("inform", "true")
                    .runnableInformer(60 * 1000L);

//            informer.stopped().whenComplete((v, t) -> {
//                if (t != null) {
//                    System.out.println("Exception occurred, caught: {}", t.getMessage());
//                }
//            });
            informer.addEventHandler(new ResourceEventHandler<>()
            {
                @Override
                public void onAdd(GenericKubernetesResource genericKubernetesResource)
                {
                    info("ADD {}/{}", genericKubernetesResource.getMetadata().getNamespace(),
                            genericKubernetesResource.getMetadata().getName());
                }

                @Override
                public void onUpdate(GenericKubernetesResource genericKubernetesResource, GenericKubernetesResource t1)
                {
                    info("UPDATE {}/{}", genericKubernetesResource.getMetadata().getNamespace(),
                            genericKubernetesResource.getMetadata().getName());
                }

                @Override
                public void onDelete(GenericKubernetesResource genericKubernetesResource, boolean b)
                {
                    info("DELETE {}/{}", genericKubernetesResource.getMetadata().getNamespace(),
                            genericKubernetesResource.getMetadata().getName());
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

    public static void animal()
            throws IOException
    {

        try (NamespacedKubernetesClient client = new DefaultKubernetesClient()) {
            System.out.println("INSIDE!");

            client.pods().inNamespace("trino-operator").list().getItems().forEach(
                    pod -> System.out.println("POD - " + pod.getMetadata().getName())
            );
            System.out.println("Finish");

            CustomResourceDefinitionContext animalCrdContext = new CustomResourceDefinitionContext.Builder()
                    .withName("animals.jungle.example.com")
                    .withGroup("jungle.example.com")
                    .withScope("Namespaced")
                    .withVersion("v1")
                    .withPlural("animals")
                    .build();

//            """
//                            apiVersion: jungle.example.com/v1
//                            kind: Animal
//                            metadata:
//                              name: walrus2
//                            spec:
//                              image: my-awesome-walrus-image"""
//            GenericKubernetesResource walrus = createWalrus(client, "abc", "walrus3");

            GenericKubernetesResource walrus = new GenericKubernetesResourceBuilder()
                    .withApiVersion("jungle.example.com/v1")
                    .withKind("Animal")
                    .withNewMetadata()
                    .withName("walrus3")
                    .endMetadata()
                    .addToAdditionalProperties("spec", Map.of("image", "dummy.image/link"))
                    .build();
            client.genericKubernetesResources(animalCrdContext)
                    .inNamespace("abc")
                    .resource(walrus)
                    .createOrReplace();

            client.genericKubernetesResources("jungle.example.com/v1", "Animal")
                    .inNamespace("abc2")
                    .resource(walrus)
                    .createOrReplace();
//            CustomResourceDefinitionList lis = client.apiextensions().v1beta1()
//                    .customResourceDefinitions()..list();

//            CustomResourceDefinition iamServiceAccount = client.apiextensions().v1beta1().customResourceDefinitions().withName("iamserviceaccounts.iam.cnrm.cloud.google.com").get();
//            CustomResourceDefinitionContext iamServiceAccountContext = CustomResourceDefinitionContext.fromCrd(iamServiceAccount);
//            System.out.println("got CTX");

        }
    }

    private static GenericKubernetesResource createWalrus(NamespacedKubernetesClient client, String namespace, String name)
    {
        GenericKubernetesResource walrus = new GenericKubernetesResourceBuilder()
                .withApiVersion("jungle.example.com/v1")
                .withKind("Animal")
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .addToAdditionalProperties("spec", Map.of("image", "dummy.image/link"))
                .build();
        client.genericKubernetesResources("jungle.example.com/v1", "Animal")
                .inNamespace(namespace)
                .resource(walrus)
                .createOrReplace();
        return walrus;
    }
}
