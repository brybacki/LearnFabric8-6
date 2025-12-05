package bryba.learn;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreateAnimals
{
    /*
kind create
    ~/ws/kubernetes-yamls                                                                                                                                                                                 11:59:34
k create -f AnimalCRD.yaml
    customresourcedefinition.apiextensions.k8s.io/animals.jungle.example.com created

k create ns abc2
    namespace/abc2 created

k create ns abc

     */

    public static void main(String[] args)
            throws InterruptedException
    {
        Config c = Config.autoConfigure("kind-kind");
        System.out.println("CreateAnimals");

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            createWalrus(client, "abc", "wt1", true);
            TimeUnit.SECONDS.sleep(5);

            createWalrus(client, "abc2", "w2");
            TimeUnit.SECONDS.sleep(5);

            createWalrus(client, "abc", "wt3", true);
            TimeUnit.SECONDS.sleep(5);

            createWalrus(client, "abc", "w4");
            TimeUnit.SECONDS.sleep(5);

            createWalrus(client, "abc2", "wt4", true);
            TimeUnit.SECONDS.sleep(5);
        }
    }

    private static GenericKubernetesResource createWalrus(KubernetesClient client, String namespace, String name)
    {
        return createWalrus(client, namespace, name, false);
    }

    private static GenericKubernetesResource createWalrus(KubernetesClient client, String namespace, String name, boolean label)
    {
        System.out.println("CreateAnimals " + namespace + "/" + name);
        Map<String, String> labels = new HashMap<>();
        if (label) {
            labels.put("inform", "true");
        }
        GenericKubernetesResource walrus = new GenericKubernetesResourceBuilder()
                .withApiVersion("jungle.example.com/v1")
                .withKind("Animal")
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
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
