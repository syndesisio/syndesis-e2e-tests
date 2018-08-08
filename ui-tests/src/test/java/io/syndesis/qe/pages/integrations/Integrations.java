package io.syndesis.qe.pages.integrations;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import io.syndesis.qe.fragments.common.list.actions.ListAction;
import io.syndesis.qe.pages.SyndesisPageObject;
import io.syndesis.qe.pages.integrations.fragments.IntegrationsList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Integrations extends SyndesisPageObject {

    private static final class Element {
        public static final By ROOT = By.cssSelector("syndesis-integration-list-page");
    }

    IntegrationsList integrationsList = new IntegrationsList(By.cssSelector("syndesis-integration-list"));

    @Override
    public SelenideElement getRootElement() {
        SelenideElement elementRoot = $(Element.ROOT).shouldBe(visible);
        return elementRoot;
    }

    @Override
    public boolean validate() {
        return getRootElement().is(visible);
    }

    public SelenideElement getIntegration(String name) {
        return integrationsList.getItem(name);
    }

    public boolean isIntegrationPresent(String name) {
        log.info("Checking if integration {} is present in the list", name);
        SelenideElement integration = getIntegration(name);
        return integration.is(visible);
    }

    public void goToIntegrationDetail(String integrationName) {
        integrationsList.invokeActionOnItem(integrationName, ListAction.CLICK);
    }

    public void deleteIntegration(String integrationName) {
        log.info("clicking delete link for integration {}", integrationName);
        integrationsList.invokeActionOnItem(integrationName, ListAction.DELETE);
    }

    public ElementsCollection getAllIntegrations() {
        return integrationsList.getItemsCollection();
    }

    public String getIntegrationName(SelenideElement integration) {
        return integrationsList.getTitleOfItem(integration);
    }

    public String getIntegrationItemStatus(SelenideElement item) {
        return integrationsList.getStatus(getIntegrationName(item));
    }

    public String getIntegrationItemStartingStatus(SelenideElement item) {
        return integrationsList.getStartingStatus(item);
    }

    public SelenideElement getKebabButtonFromItem(SelenideElement item) {
        return integrationsList.getKebabButton(item);
    }

    public void checkKebabActions(SelenideElement item, String status) {
        integrationsList.getKebabMenu(item).open();
        integrationsList.getKebabMenu(item).checkActionsSet(getKebabActionsByStatus(status));
    }

    public String[] getKebabActionsByStatus(String status) {
        String[] actions;

        String view = "View";
        String edit = "Edit";
        String deactivate = "Deactivate";
        String activate = "Activate";
        String deleteAction = "Delete";

        switch (status) {
            case "Active":
                actions = new String[] {view, edit, deactivate, deleteAction};
                break;
            case "Inactive":
            case "Draft":
                actions = new String[] {view, edit, activate, deleteAction};
                break;
            case "In Progress":
                actions = new String[] {view, edit, deleteAction};
                break;
            default:
                actions = new String[] {};
                break;
        }

        return actions;
    }

    public void checkAllIntegrationsKebabButtons() {

        for (SelenideElement item : integrationsList.getItemsCollection()) {
            String status = integrationsList.getStatus(item);

            if (status.equals("Deleted")) {
                integrationsList.getKebabButton(item).shouldBe(hidden);
            } else {
                checkKebabActions(item, status);
            }
        }
    }
}

