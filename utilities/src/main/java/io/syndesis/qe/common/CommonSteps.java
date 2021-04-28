package io.syndesis.qe.common;

import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.component.ComponentUtils;
import io.syndesis.qe.resource.ResourceFactory;
import io.syndesis.qe.resource.impl.Syndesis;
import io.syndesis.qe.test.InfraFail;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.PortForwardUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.xtf.core.config.WaitingConfig;
import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.openshift.crd.CustomResourceDefinitionContextProvider;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.core.waiting.Waiter;
import cz.xtf.core.waiting.WaiterException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonSteps {
    public static void cleanNamespace() {
        log.info("Cleaning namespace");
        undeploySyndesis();
        OpenShiftUtils.getInstance().templates().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.getInstance().apps().statefulSets().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.getInstance().apps().deployments().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.getInstance().serviceAccounts().withName("syndesis-oauth-client").delete();
        try {
            OpenShiftUtils.getInstance().clean();
            //OpenShiftUtils.getInstance().waiters().isProjectClean().waitFor(); //workaround for 4.7 is need. see description of the next function
            // workaround for Jaeger installed by OLM
            OpenShiftUtils.binary().execute("delete", "subscriptions", "jaeger-product");
            OpenShiftUtils.binary().execute("delete", "csv", "jaeger-operator.v1.20.3");

            isProjectClean().waitFor();
        } catch (WaiterException e) {
            log.warn("Project was not clean after 20s, retrying once again");
            OpenShiftUtils.getInstance().clean();
            //OpenShiftUtils.getInstance().waiters().isProjectClean().waitFor(); //workaround for 4.7 is need. see description of the next function
            isProjectClean().waitFor();
        }
        OpenShiftUtils.getInstance().getTemplates().forEach(OpenShiftUtils.getInstance()::deleteTemplate);
        PortForwardUtils.reset();
    }

    public static void deploySyndesis() {
        log.info("Deploying Syndesis to namespace");
        ResourceFactory.get(Syndesis.class).deploy();
        // Use this method instead of ResourceFactory#create() to get the info what went wrong
        waitForSyndesis();
    }

    public static void waitForSyndesis() {
        try {
            log.info("Waiting for Syndesis to get ready");
            OpenShiftWaitUtils.waitFor(() -> ResourceFactory.get(Syndesis.class).isReady(), 10000L, 15 * 60000L);
            PortForwardUtils.createOrCheckPortForward();
        } catch (TimeoutException | InterruptedException e) {
            log.error("Was waiting for following syndesis components:");
            ComponentUtils.getAllComponents().forEach(c -> log.error("  " + c.getName()));
            log.error("Found following component pods:");
            ComponentUtils.getComponentPods().forEach(p -> log.error("  " + p.getMetadata().getName()
                + " [ready: " + OpenShiftWaitUtils.isPodReady(p) + "]"));
            InfraFail.fail("Wait for Syndesis failed, check error logs for details.", e);
        }
    }

    public static void undeploySyndesis() {
        ResourceFactory.get(Syndesis.class).undeployCustomResources();
        try {
            OpenShiftWaitUtils.waitFor(() -> ResourceFactory.get(Syndesis.class).isUndeployed(), 10 * 60000L);
        } catch (TimeoutException | InterruptedException e) {
            log.error("Was waiting until there is only operator pod or no pods");
            log.error("Found following component pods:");
            ComponentUtils.getComponentPods().forEach(p -> log.error("  " + p.getMetadata().getName()));
            InfraFail.fail("Wait for Syndesis undeployment failed, check error logs for details.", e);
        }
    }

    /**
     * Workaround for isProjectClean function from XTF. See issue: https://github.com/xtf-cz/xtf/issues/406
     * XTF cannot be update because it uses openshift-client 4.13.0 which contains kubernetes-model 4.11.2 where CRD was moved to the `v1beta1` .
     * Since Syndesis endpoints uses old kubernetes-model, the CRD is missing (`NoClassDefFoundError`)
     * When we let Syndesis endpoints to bring the old kubernetes-model with itself (remove <exclude> from parent pom.xml),
     * it causes undesired behaviour, e.g. portForwarding doesn't work correctly etc.
     * So we need to wait with bump xtf version until the Syndesis will contains newer kubernetes-model
     * <p>
     * This is implementation from XTF with workaround
     * To access to the protected methods, the JavaReflection API is used.
     */
    private static Waiter isProjectClean() {
        return new SimpleWaiter(() -> {
            int crdInstances = 0;
            int removableResources = 0;
            try {
                Method privateMethodGetCRDContextProviders = OpenShift.class.getDeclaredMethod("getCRDContextProviders", null);
                privateMethodGetCRDContextProviders.setAccessible(true);
                Method privateMethodListRemovableResources = OpenShift.class.getDeclaredMethod("listRemovableResources", null);
                privateMethodListRemovableResources.setAccessible(true);
                ServiceLoader<CustomResourceDefinitionContextProvider> cRDContextProviders =
                    (ServiceLoader<CustomResourceDefinitionContextProvider>) privateMethodGetCRDContextProviders.invoke(OpenShift.class,
                        null);
                for (CustomResourceDefinitionContextProvider crdContextProvider : cRDContextProviders) {
                    try {
                        crdInstances += ((List) (OpenShiftUtils.getInstance().customResource(crdContextProvider.getContext())
                            .list(OpenShiftUtils.getInstance().getNamespace()).get("items"))).size();
                    } catch (KubernetesClientException kce) {
                        // CRD might not be installed on the cluster
                    }
                }
                List<HasMetadata> removableResourcesList =
                    (List<HasMetadata>) privateMethodListRemovableResources.invoke(OpenShiftUtils.getInstance(), null);
                removableResources = removableResourcesList.size();
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            log.info(String.format("Testing if project is clean, CRD instances: %d , RemovableResources: %d", crdInstances, removableResources));
            return crdInstances == 0 &
                removableResources <= 3;
            // +1 because configMap can be there on OCP 4.7, see https://github.com/xtf-cz/xtf/issues/406 ,
            // +2 resources from jaeger operator if there was a jaeger in this namespace before and there is another namespace with jaeger operator
        }, TimeUnit.MILLISECONDS, WaitingConfig.timeoutCleanup(), "Cleaning project.");
    }
}
