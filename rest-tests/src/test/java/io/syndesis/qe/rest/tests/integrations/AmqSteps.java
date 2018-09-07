package io.syndesis.qe.rest.tests.integrations;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import io.syndesis.common.model.DataShapeKinds;
import io.syndesis.common.model.action.Action;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.Connection;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.qe.bdd.AbstractStep;
import io.syndesis.qe.bdd.entities.StepDefinition;
import io.syndesis.qe.bdd.storage.StepsStorage;
import io.syndesis.qe.endpoints.ConnectionsEndpoint;
import io.syndesis.qe.endpoints.ConnectorsEndpoint;
import io.syndesis.qe.utils.RestConstants;
import io.syndesis.qe.utils.TestUtils;

public class AmqSteps extends AbstractStep {
    @Autowired
    private StepsStorage steps;
    @Autowired
    private ConnectionsEndpoint connectionsEndpoint;
    @Autowired
    private ConnectorsEndpoint connectorsEndpoint;

    private Connector amqConnector;
    private Connection amqConnection;
    private Action connectorAction;
    private Map<String, String> properties;

    public AmqSteps() {
    }

    private void init(String action, String destinationType, String destinationName) {
        amqConnector = connectorsEndpoint.get("activemq");
        amqConnection = connectionsEndpoint.get(RestConstants.AMQ_CONNECTION_ID);
        connectorAction = TestUtils.findConnectorAction(amqConnector, action);
        properties = TestUtils.map(
                "destinationType", destinationType.toLowerCase().equals("queue") ? "queue" : "topic",
                "destinationName", destinationName
        );
    }

    @Given("^create AMQ \"([^\"]*)\" action step with destination type \"([^\"]*)\" and destination name \"([^\"]*)\"$")
    public void createAmqStep(String action, String destinationType, String destinationName) {
        init(action, destinationType, destinationName);

        final Step amqStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .id(UUID.randomUUID().toString())
                .connection(amqConnection)
                .action(connectorAction)
                .configuredProperties(properties)
                .build();

        steps.getStepDefinitions().add(new StepDefinition(amqStep));
    }

    @When("^create AMQ \"([^\"]*)\" action step with destination type \"([^\"]*)\" and destination name \"([^\"]*)\" with " +
            "datashape type \"([^\"]*)\" and specification \'([^\']*)\'$")
    public void createAmqStepWithDatashape(String action, String destinationType, String destinationName, String datashapeType, String datashape) {
        init(action, destinationType, destinationName);

        final ConnectorDescriptor connectorDescriptor = getConnectorDescriptor(connectorAction, properties, RestConstants.AMQ_CONNECTION_ID);
        final Step amqStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .id(UUID.randomUUID().toString())
                .connection(amqConnection)
                .action(
                        withCustomDatashape(
                                connectorAction,
                                connectorDescriptor,
                                "in",
                                DataShapeKinds.valueOf(datashapeType),
                                datashape))
                .configuredProperties(properties)
                .build();

        steps.getStepDefinitions().add(new StepDefinition(amqStep));
    }
}
