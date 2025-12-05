package bryba.learn;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class CreateNamespace
{
    public static void main(String[] args)
    {
        Config c = Config.autoConfigure("kind-kind");
        System.out.println("CreateAnimals");

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            client.namespaces().createOrReplace(
                    new NamespaceBuilder()
                            .withMetadata(new ObjectMetaBuilder()
                                    .withName("abc")
                                    .build())
                            .build());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
