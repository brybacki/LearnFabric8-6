package bryba.learn;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import software.amazon.awssdk.utils.ImmutableMap;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PodAnnotations
{
    /*
kind create cluster --name replica (or kind)
    ~/ws/kubernetes-yamls                                                                                                                                                                                 11:59:34
k create -f AnimalCRD.yaml
    customresourcedefinition.apiextensions.k8s.io/animals.jungle.example.com created

k create ns abc2
    namespace/abc2 created

k create ns abc

     */

    /*
     * trino-coordinator-7554474b64-tfplj   1708m        1754Mi
     * trino-worker-69895db649-jj2hz        3115m        42152Mi
     * trino-worker-69895db649-wsddg        11585m       37542Mi
     * trino-worker-69895db649-ww2lt        542m         35063Mi
     * trino-worker-69895db649-xrb5w        5329m        47201Mi
     */
    public static void main(String[] args)
            throws InterruptedException
    {
        Config cfg = Config.autoConfigure("kind-replica");
//        Config cfg = Config.autoConfigure("aws-us-east1-5824");
        System.out.println("CreateAnimals");

//        Namespace myns = client.namespaces().create(new NamespaceBuilder()
//                .withNewMetadata()
//                .withName("myns")
//                .addToLabels("a", "label")
//                .endMetadata()
//                .build());

//        createPods(c);
        String namespace = "abc";
//        String namespace = "trino-a-67687314-w-2576265201-dep-953479217910";
//        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(cfg).build()) {
//            addPodAnnotations(client, namespace, "p-3", ImmutableMap.of("B", "Fv"));
//        }
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(cfg).build()) {
            removePodAnnotations(client, namespace, "p-1", ImmutableMap.of("A", null, "B", null));
            removePodAnnotations(client, namespace, "p-3", ImmutableMap.of("A", null, "B", null));
        }
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(cfg).build()) {
//            fabric8Example(client, namespace);

            System.out.println();
            PodList pods = client.pods().inNamespace(namespace).list();

            System.out.println();
            System.out.println("==== Pods Annotations ====");
            for (Pod pod : pods.getItems()) {
                System.out.printf("{%-45s} ", pod.getMetadata().getName());
                String name = pod.getMetadata().getName();

                Map<String, String> annotations = pod.getMetadata().getAnnotations();
                System.out.println();
                annotations.forEach((k, v) -> System.out.printf("    %s: %s\n", k, v));
            }
        }
    }

    public static void addPodAnnotations(KubernetesClient client, String namespace, String podName, Map<String, String> annotations)
    {
        client.pods().inNamespace(namespace)
//                .withLabels(deploymentLabels)
                .resources()
                .filter(podResource -> podResource.get().getMetadata().getName().equals(podName))
                .findFirst()
                .ifPresent(podResource ->
                        podResource.edit(pod -> updatePodAnnotations(pod, annotations)));
    }

    private static Pod updatePodAnnotations(Pod pod, Map<String, String> annotations)
    {
        return new PodBuilder(pod)
                .editOrNewMetadata()
                .addToAnnotations(annotations)
                .endMetadata()
                .build();
    }

    public static void removePodAnnotations(KubernetesClient client, String namespace, String podName, Map<String, String> annotations)
    {
        client.pods().inNamespace(namespace)
//                .withLabels(deploymentLabels)
                .resources()
                .filter(podResource -> podResource.get().getMetadata().getName().equals(podName))
                .findFirst()
                .ifPresent(podResource ->
                        podResource.edit(pod -> removePodAnnotation(pod, annotations)));
    }

    private static Pod removePodAnnotation(Pod pod, Map<String, String> annotations)
    {
        return new PodBuilder(pod)
                .editOrNewMetadata()
                .removeFromAnnotations(annotations)
                .endMetadata()
                .build();
    }

    private static void createPods(Config c)
    {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(c).build()) {
            createNamespace(client, "abc");
            createPod(client, "abc", "p-1");
            createPod(client, "abc", "p-2");
            createPod(client, "abc", "p-3");
        }
    }

    private static void createNamespace(KubernetesClient client, String name)
    {
        Namespace namespace = new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();
        client.namespaces().resource(namespace).create();
    }

    // -> https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#pods
    private static Pod createPod(KubernetesClient client, String namespace, String name)
    {
        System.out.println("CreatePod: " + namespace + "/" + name);
        Map<String, String> labels = new HashMap<>();
//        if (label) {
//            labels.put("inform", "true");
//        }

        Pod nginxPod = new PodBuilder()
                .withNewMetadata().withName(name).endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("nginx")
                .withImage("nginx:1.7.9")
                .addNewPort().withContainerPort(80).endPort()
                .endContainer()
                .endSpec()
                .build();

        Pod pod = client.pods().inNamespace(namespace)
                .resource(nginxPod).create();

        //client.pods().inNamespace(namespace).withName(name).edit();
        return pod;
    }

    /*
    POD:

apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx:1.14.2
    ports:
    - containerPort: 80
     */
}
