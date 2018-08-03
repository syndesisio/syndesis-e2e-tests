package io.syndesis.qe.templates;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FtpTemplate {
    private static final String APP_NAME = "ftpd";
    private static final String LABEL_NAME = "app";

    public static void deploy() {
        if (!TestUtils.isDcDeployed("ftpd")) {
            List<ContainerPort> ports = new LinkedList<>();
            ports.add(new ContainerPortBuilder()
                    .withName("ftp-cmd")
                    .withContainerPort(2121)
                    .withProtocol("TCP").build());

            for (int i = 0; i < 10; i++) {
                ContainerPort dataPort = new ContainerPortBuilder()
                        .withName("ftp-data-" + i)
                        .withContainerPort(2300 + i)
                        .withProtocol("TCP")
                        .build();
                ports.add(dataPort);
            }

            OpenShiftUtils.client().deploymentConfigs().createOrReplaceWithNew()
                    .editOrNewMetadata()
                    .withName(APP_NAME)
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()

                    .editOrNewSpec()
                    .addToSelector(LABEL_NAME, APP_NAME)
                    .withReplicas(1)
                    .editOrNewTemplate()
                    .editOrNewMetadata()
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()
                    .editOrNewSpec()
                    .addNewContainer().withName(APP_NAME).withImage("dsimansk/ftpd:latest").addAllToPorts(ports)

                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .addNewTrigger()
                    .withType("ConfigChange")
                    .endTrigger()
                    .endSpec()
                    .done();

            ServiceSpecBuilder serviceSpecBuilder = new ServiceSpecBuilder().addToSelector(LABEL_NAME, APP_NAME);

            serviceSpecBuilder.addToPorts(new ServicePortBuilder()
                    .withName("ftp-cmd")
                    .withPort(2121)
                    .withTargetPort(new IntOrString(2121))
                    .build());

            for (int i = 0; i < 10; i++) {
                serviceSpecBuilder.addToPorts(new ServicePortBuilder()
                        .withName("ftp-data-" + i)
                        .withPort(2300 + i)
                        .withTargetPort(new IntOrString(2300 + i))
                        .build());
            }

            OpenShiftUtils.getInstance().client().services().createOrReplaceWithNew()
                    .editOrNewMetadata()
                    .withName(APP_NAME)
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()
                    .editOrNewSpecLike(serviceSpecBuilder.build())
                    .endSpec()
                    .done();

            try {
                OpenShiftWaitUtils.waitFor(OpenShiftWaitUtils.areExactlyNPodsReady(LABEL_NAME, APP_NAME, 1));
                Thread.sleep(20 * 1000);
            } catch (InterruptedException | TimeoutException e) {
                log.error("Wait for {} deployment failed ", APP_NAME, e);
            }
        }
        Account ftpAccount = new Account();
        ftpAccount.setService("ftp");
        Map<String, String> accountParameters = new HashMap<>();
        accountParameters.put("host", "ftpd");
        accountParameters.put("port", "2121");
        ftpAccount.setProperties(accountParameters);
        AccountsDirectory.getInstance().addAccount("FTP", ftpAccount);
    }

    public static void cleanUp() {
        OpenShiftUtils.getInstance().getDeploymentConfigs().stream().filter(dc -> dc.getMetadata().getName().equals(APP_NAME)).findFirst()
                .ifPresent(dc -> OpenShiftUtils.getInstance().deleteDeploymentConfig(dc, true));
        OpenShiftUtils.getInstance().getServices().stream().filter(service -> APP_NAME.equals(service.getMetadata().getName())).findFirst()
                .ifPresent(service -> OpenShiftUtils.getInstance().deleteService(service));
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
