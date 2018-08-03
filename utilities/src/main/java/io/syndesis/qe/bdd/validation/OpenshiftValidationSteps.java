package io.syndesis.qe.bdd.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import io.syndesis.qe.templates.AmqTemplate;
import io.syndesis.qe.templates.FtpTemplate;
import io.syndesis.qe.templates.HTTPEndpointsTemplate;
import io.syndesis.qe.templates.KafkaTemplate;
import io.syndesis.qe.templates.MysqlTemplate;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenshiftValidationSteps {
    @Then("^wait until mysql database starts$")
    public void waitUntilDatabaseStarts() {
        MysqlTemplate.waitUntilMysqlIsReady();
    }

    @Then("^check that pod \"([^\"]*)\" logs contain string \"([^\"]*)\"$")
    public void checkPodHasInLog(String podPartialName, String expectedText) {
        assertThat(OpenShiftUtils.getPodLogs(podPartialName))
                .containsIgnoringCase(expectedText);
    }

    @Given("^deploy FTP server$")
    public void deployFTPServier() {
        FtpTemplate.deploy();
    }

    @Given("^clean FTP server$")
    public void cleanFTPServier() {
        FtpTemplate.cleanUp();
    }

    @Given("^clean MySQL server$")
    public void cleanMySQLServer() {
        MysqlTemplate.cleanUp();
    }

    @Given("^deploy MySQL server$")
    public void deployMySQLServer() {
        MysqlTemplate.deploy();
    }

    @Given("^deploy AMQ broker and add accounts$")
    public void deployAMQBroker() {
        AmqTemplate.deploy();
    }

    @Given("^deploy AMQ broker if it doesnt exist$")
    public void deployAMQBrokerIfMissing() {
        AmqTemplate.deploy();
    }

    @Given("^deploy Kafka broker and add account$")
    public void deployKafka() {
        KafkaTemplate.deploy();
    }

    @Given("^deploy HTTP endpoints")
    public void deployHTTPEndpoints() {
        HTTPEndpointsTemplate.deploy();
    }

    @And("^wait until \"([^\"]*)\" pod is reloaded$")
    public void waitUntilPodIsReloaded(String podName) {
        try {
            OpenShiftWaitUtils.waitForPodIsReloaded(podName);
        } catch (InterruptedException | TimeoutException e) {
            fail(e.getMessage());
        }
    }

}
