package io.syndesis.qe.steps.integrations.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.is;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.enabled;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;

import java.util.List;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.syndesis.qe.pages.integrations.editor.Editor;
import io.syndesis.qe.pages.integrations.editor.add.ChooseConnection;
import io.syndesis.qe.pages.integrations.fragments.IntegrationFlowView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EditorSteps {

    private Editor editor = new Editor();
    private IntegrationFlowView flowViewComponent = new IntegrationFlowView();
    private ChooseConnection chooseConnection = new ChooseConnection();

    private static final class Element {
        public static final By EXPANDER = By.xpath("//button[contains(@class, 'toggle-collapsed')]");
        public static final By HIDDEN_DETAILED_VIEW = By.cssSelector("div[class*='flow-view-container syn-scrollable--body collapsed']");
    }

    private static final class Button {
        public static final By Publish = By.xpath("//button[text()[contains(.,'Publish')]]");
        public static final By SaveAsDraft = By.xpath("//button[text()[contains(.,'Save')]]");
        public static final By Cancel = By.xpath("//button[text()[contains(.,'Save')]]");
    }

    @Then("^check visibility of visual integration editor$")
    public void verifyNewIntegrationEditorOpened() {
        editor.getRootElement().shouldBe(visible);
        chooseConnection.getRootElement().shouldBe(visible);
        flowViewComponent.getRootElement().shouldBe(visible);
    }

    @Then("^check visibility of visual integration editor for \"([^\"]*)\"$")
    public void verifyEditorOpenedFor(String integrationName) {
        this.verifyNewIntegrationEditorOpened();
        log.info("editor must display integration name {}", integrationName);
        assertThat(flowViewComponent.getIntegrationName(), is(integrationName));
    }

    @Deprecated
    @When("^add first step between START and STEP connection$")
    public void sheAddsFirstStep() {
        flowViewComponent.clickAddStepLink(0);
    }

    @Deprecated
    @When("^add second step between STEP and FINISH connection$")
    public void sheAddsSecond() {
        flowViewComponent.clickAddStepLink(1);
    }

    @When("^.*adds? integration step on position \"([^\"]*)\"$")
    public void addAnotherStep(int stepPos) {
        flowViewComponent.clickAddStepLink(stepPos);
    }

    @Then("^verify delete button on step ([0-9]+) (is|is not) visible$")
    public void verifyDeleteButtonOnStartFinishStepNotVisible(int position, String visibility) throws Throwable {
        SelenideElement trashIcon = flowViewComponent.getStepOnPosition(position).$(By.className("fa-trash"));
        assertEquals("Delete icon should" + visibility.replace("is", "") + " be visible",
                "is".equals(visibility),
                trashIcon.exists()
        );
    }

    /**
     * unused
     */
    @Then("^.*checks? that text \"([^\"]*)\" is \"([^\"]*)\" in hover table over \"([^\"]*)\" step$")
    public void checkTextInHoverTable(String text, String isVisible, String stepPosition) throws InterruptedException {
        if (isVisible.equalsIgnoreCase("visible")) {
            Assertions.assertThat(flowViewComponent.checkTextInHoverTable(stepPosition))
                    .isNotEmpty()
                    .containsIgnoringCase(text);
        } else {
            Assertions.assertThat(flowViewComponent.checkTextInHoverTable(stepPosition))
                    .isNotEmpty()
                    .doesNotContain(text);
        }
    }

    /**
     * Every step element has class step and every option to add step/connection also has class step.
     * So if you have 3 steps created, stepPosition is: first = 0, second = 2, third = 4 etc.
     *
     * @param text
     * @param isVisible
     * @param stepPosition
     * @throws InterruptedException
     */
    @Then("^.*checks? that text \"([^\"]*)\" is \"([^\"]*)\" in step warning inside of step number \"([^\"]*)\"$")
    public void checkTextInStepWarning(String text, String isVisible, int stepPosition) {
        if (isVisible.equalsIgnoreCase("visible")) {
            doCheckTextInStepsWarningTable(text, stepPosition, true);
        } else {
            doCheckTextInStepsWarningTable(text, stepPosition, false);
        }
    }

    @Then("^.*checks? that text \"([^\"]*)\" is \"([^\"]*)\" in step warning inside of steps$")
    public void checkTextInStepsWarning(String text, String isVisible, DataTable table) {
        for (String index : table.asList()) {
            if (isVisible.equalsIgnoreCase("visible")) {
                doCheckTextInStepsWarningTable(text, Integer.valueOf(index), true);
            } else {
                doCheckTextInStepsWarningTable(text, Integer.valueOf(index), false);
            }
        }
    }

    public void doCheckTextInStepsWarningTable(String text, int position, boolean visible) {
        if (visible) {
            Assertions.assertThat(flowViewComponent.getWarningTextFromStep(position))
                    .isNotEmpty()
                    .containsIgnoringCase(text);
        } else {
            Assertions.assertThat(flowViewComponent.getWarningTextFromStep(position))
                    .isNotEmpty()
                    .doesNotContain(text);
        }
    }

    @Then("^.*checks? that in connection info popover for step number \"([^\"]*)\" is following text$")
    public void checkTextInConnectionInfo(int stepPosition, DataTable connectionsData) {
        List<String> data = connectionsData.asList(String.class);
        String foundText = flowViewComponent.getConnectionPropertiesText(flowViewComponent.getStepOnPosition(stepPosition));

        Assertions.assertThat(foundText).isNotEmpty();

        for (String column : data) {
            Assertions.assertThat(foundText)
                    .containsIgnoringCase(column);
        }
    }

    /**
     * Every step element has class step and every option to add step/connection also has class step.
     * If you have 3 steps created, position is: first = 0, second = 2, third = 4 etc.
     *
     * @param position index of element with class .step
     */
    @Then(".*checks? that there is no warning inside of step number \"([^\"]*)\"$")
    public void checkIfWarningIsVisible(int position) {
        Assertions.assertThat(flowViewComponent.getStepWarningElement(position).isDisplayed()).isFalse();
    }

    @Then(".*checks? that there is no warning inside of steps in range from \"([^\"]*)\" to \"([^\"]*)\"$")
    public void checkIfWarningIsVisibleInRange(int start, int finish) {
        for (int i = start; i <= finish; i++) {
            Assertions.assertThat(flowViewComponent.getStepWarningElement(i).isDisplayed()).isFalse();
        }
    }

    @When("^open integration flow details")
    public void openIntegrationFlowDetails() {
        if (editor.getRootElement().$(Element.HIDDEN_DETAILED_VIEW).exists()) {
            log.info("Expander is closed, opening details");
            editor.getRootElement().$(Element.EXPANDER).shouldBe(visible).click();
        }
    }

    @Then("^check flow title is \"([^\"]*)\"$")
    public void checkFlowTitleIs(String title) throws Throwable {
        assertEquals("Wrong flow title", title, flowViewComponent.getFlowTitle());
    }

    @Then("^check there are (\\d+) integration steps$")
    public void checkNumberOfIntegrationSteps(int n) {
        assertEquals("Wrong number of steps", n, flowViewComponent.getNumberOfSteps());
    }

    @Then("^check there is a step with \"([^\"]*)\" title")
    public void checkStepTitle(String title) {
        Assertions.assertThat(flowViewComponent.getStepsTitlesArray()).contains(title);
    }

    @Then("^check that (\\w+). step has ([^\"]*) title")
    public void checkParticularStepTitle(int positionOfStep, String title) {
        Assertions.assertThat(flowViewComponent.getStepsTitlesArray().get(positionOfStep - 1)).contains(title);
    }

    @When("^edit integration step on position (\\d+)$")
    public void editIntegrationStep(int oneBasedStepPosition) {
        log.info("Editing integration step #" + oneBasedStepPosition);
        flowViewComponent.getStepOnPosition(oneBasedStepPosition).$(".step-content").shouldBe(visible).click();
    }

    @When("^delete step on position (\\d+)$")
    public void deleteStepOnPosition(int oneBasedStepPosition) throws Throwable {
        log.info("Deleting integration step #" + oneBasedStepPosition);
        flowViewComponent.deleteStepOnPostion(oneBasedStepPosition);
    }

    @When("^publish integration$")
    public void publishIntegration() {
        log.info("Publishing integration");
        editor.getRootElement().$(Button.Publish).shouldBe(visible).click();
    }
}
