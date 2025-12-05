package bryba.learn;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CreatePods
{
    /*
kind create cluster --name replica (or kind)
    ~/ws/kubernetes-yamls                                                                                                                                                                                 11:59:34
k create -f AnimalCRD.yaml
    customresourcedefinition.apiextensions.k8s.io/animals.jungle.example.com created

k create ns abc2
    namespace/abc2 created

k create ns abc

----
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.5.0/components.yaml
kubectl patch -n kube-system deployment metrics-server --type=json \
  -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

     */
    private static final String CPU = "cpu";
    private static final String MEMORY = "memory";

    private static class CpuLoad
    {
        double used;
        double max;

        double getLoad()
        {
            return max <= 0.0 ? 0.0 : used / max;
        }
    }

    public static void main(String[] args)
    {
        Config cfg = Config.autoConfigure("kind-replica");
        createPods(cfg);
        System.out.println("Labeling pods");
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(cfg).build()) {
            client.pods().inNamespace("abc").list().getItems().forEach(pod ->
            {
                String nodeName = pod.getSpec().getNodeName();
                String podName = pod.getMetadata().getName();
                System.out.printf("Found pod %s, with nodeName %s\n", podName, nodeName);

                addLabels(client, nodeName, podName);
            });
        }
    }

    private static Node addLabels(KubernetesClient client, String nodeName, String podName)
    {
        Resource<Node> nodeResource = client.nodes().withName(nodeName);
        Map<String, String> lbls = nodeResource.get().getMetadata().getLabels();
        System.out.println(lbls);

        return nodeResource.edit(node -> {
            node.getMetadata().getLabels().put("podek", "podek");
            return node;
        });
    }

    /*
     * trino-coordinator-7554474b64-tfplj   1708m        1754Mi
     * trino-worker-69895db649-jj2hz        3115m        42152Mi
     * trino-worker-69895db649-wsddg        11585m       37542Mi
     * trino-worker-69895db649-ww2lt        542m         35063Mi
     * trino-worker-69895db649-xrb5w        5329m        47201Mi
     */
    public static void maain(String[] args)
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
        String namespace = "kube-system";
//        String namespace = "trino-a-67687314-w-2576265201-dep-953479217910";
        Map<String, CpuLoad> cpuLoads = new HashMap<>();
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(cfg).build()) {
//            fabric8Example(client, namespace);

            System.out.println();
            System.out.println("==== Pods CPU Metrics ====");
            PodMetricsList podMetricsList = client.top().pods()./*withLabels(trinoWorkerSelector).*/inNamespace(namespace).metrics();
            podMetricsList.getItems().stream().forEach(pod -> {
                // filter by "trino-worker" container!
                Optional<BigDecimal> sum = pod.getContainers().stream()
                        .map(containerMetrics -> containerMetrics.getUsage().get(CPU).getNumericalAmount())
                        .reduce(BigDecimal::add);
                BigDecimal usage = sum.orElse(BigDecimal.ZERO);
                System.out.printf("{%-45s} %s\n", pod.getMetadata().getName(), usage);
                cpuLoads.computeIfAbsent(pod.getMetadata().getName(), s -> new CpuLoad()).used = usage.doubleValue();
            });

            PodList pods = client.pods().inNamespace(namespace).list();
            System.out.println();
            System.out.println("==== Pods Resource Claims ====");
            for (Pod pod : pods.getItems()) {
                System.out.printf("{%-45s} ", pod.getMetadata().getName());
                String name = pod.getMetadata().getName();
                Optional<BigDecimal> sum = pod.getSpec().getContainers().stream()
                        .map(c -> {
                            Quantity lim = c.getResources().getLimits().getOrDefault(CPU, Quantity.fromNumericalAmount(BigDecimal.ZERO, null));
                            Quantity req = c.getResources().getRequests().getOrDefault(CPU, Quantity.fromNumericalAmount(BigDecimal.ZERO, null));

                            BigDecimal ret = lim.getNumericalAmount().max(req.getNumericalAmount());
                            return ret;
                        }).reduce(BigDecimal::add);

                BigDecimal max = sum.orElse(BigDecimal.ZERO);
                cpuLoads.computeIfAbsent(name, s -> new CpuLoad()).max = max.doubleValue();

                System.out.println(max.doubleValue());
            }

            System.out.println();
            System.out.println("==== HashMap ====");
            cpuLoads.forEach((k, v) -> System.out.printf("{%-45s} - %s\n", k, v.getLoad()));
        }
    }

    private static void fabric8Example(KubernetesClient client, String namespace)
    {
        System.out.println("==== Pod Metrics ====");
        client.top().pods().metrics(namespace).getItems()
                .forEach(podMetrics -> podMetrics.getContainers()
                        .forEach(containerMetrics -> System.out.printf("{%-45s}\t\t%-20s\tCPU: %s{%s}\tMemory: %s{%s}\n",
                                podMetrics.getMetadata().getName(), containerMetrics.getName(),
                                containerMetrics.getUsage().get(CPU).getNumericalAmount().toString(), containerMetrics.getUsage().get(CPU).getFormat(),
                                containerMetrics.getUsage().get(MEMORY).getAmount(), containerMetrics.getUsage().get(MEMORY).getFormat())));

        client.pods().inNamespace(namespace).list().getItems().stream().findFirst().map(pod -> {
            System.out.printf("==== Individual Pod Metrics ({%s}) ====\n", pod.getMetadata().getName());
            try {
                return client.top().pods().metrics(namespace, pod.getMetadata().getName());
            }
            catch (KubernetesClientException ex) {
                if (ex.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    System.out.println(" - Pod has not reported any metrics yet");
                }
                else {
                    System.out.printf(" - Error retrieving Pod metrics: {%s}\n", ex.getMessage());
                }
                return null;
            }
        }).ifPresent(podMetrics -> podMetrics.getContainers()
                .forEach(containerMetrics -> System.out.printf("{%-45s}\t\t%-20s\tCPU: %s{%s}\tMemory: %s{%s}\n",
                        podMetrics.getMetadata().getName(), containerMetrics.getName(),
                        containerMetrics.getUsage().get(CPU), containerMetrics.getUsage().get(CPU).getFormat(),
                        containerMetrics.getUsage().get(MEMORY).getAmount(), containerMetrics.getUsage().get(MEMORY).getFormat())));
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
