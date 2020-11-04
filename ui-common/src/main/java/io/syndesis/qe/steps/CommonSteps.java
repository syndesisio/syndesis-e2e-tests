package io.syndesis.qe.steps;

import static io.syndesis.qe.wait.OpenShiftWaitUtils.waitFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.MapEntry.entry;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import io.syndesis.qe.CustomWebDriverProvider;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.account.Account;
import io.syndesis.qe.account.AccountsDirectory;
import io.syndesis.qe.fragments.common.form.Form;
import io.syndesis.qe.pages.ModalDialogPage;
import io.syndesis.qe.pages.SyndesisPage;
import io.syndesis.qe.pages.SyndesisRootPage;
import io.syndesis.qe.pages.connections.Connections;
import io.syndesis.qe.pages.login.GitHubLogin;
import io.syndesis.qe.pages.login.MinishiftLogin;
import io.syndesis.qe.pages.login.RHDevLogin;
import io.syndesis.qe.report.selector.ExcludeFromSelectorReports;
import io.syndesis.qe.resource.ResourceFactory;
import io.syndesis.qe.resource.impl.Syndesis;
import io.syndesis.qe.steps.connections.wizard.phases.ConfigureConnectionSteps;
import io.syndesis.qe.steps.connections.wizard.phases.NameConnectionSteps;
import io.syndesis.qe.steps.connections.wizard.phases.SelectConnectionTypeSteps;
import io.syndesis.qe.utils.Alert;
import io.syndesis.qe.utils.ByUtils;
import io.syndesis.qe.utils.CalendarUtils;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.PortForwardUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.utils.UIUtils;
import io.syndesis.qe.utils.google.GoogleAccount;
import io.syndesis.qe.utils.google.GoogleAccounts;
import io.syndesis.qe.wait.OpenShiftWaitUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import mmuzikar.api.ISuggestionProvider;
import mmuzikar.api.Suggestion;

@Slf4j
public class CommonSteps {

    private SyndesisRootPage syndesisRootPage = new SyndesisRootPage();
    private ModalDialogPage modalDialogPage = new ModalDialogPage();

    private enum Credentials {
        DROPBOX,
        SALESFORCE,
        TWITTER,
        OPENSHIFT,
        SYNDESIS,
        SLACK,
        S3,
        FTP,
        SFTP,
        GOOGLE_MAIL,
        GOOGLE_SHEETS,
        GOOGLE_CALENDAR,
        SAP_CONCUR,
        AWS,
        TELEGRAM,
        SERVICENOW,
        BOX,
        SEND_EMAIL_SMTP,
        RECEIVE_EMAIL_IMAP_OR_POP3,
        GITHUB,
        ZENHUB,
        JIRA_HOOK,
        AMQ,
        AMQP,
        MQTT,
        AWS_DDB
    }

    private static class Element {
        public static final By LOGIN_BUTTON = By.className("btn");
        public static final By NAVIGATION_PANEL = By.className("pf-c-nav");
        public static final By NAVIGATION_USER_DROPDOWN = ByUtils.dataTestId("app-top-menu-user-dropdown");
        public static final By HELP_DROPDOWN_BUTTON = ByUtils.dataTestId("helpDropdownButton");
        public static final By DROPDOWN_MENU = By.className("pf-c-dropdown__menu");

        public static final By navigationLink(String title) {
            final String dataTestid = String.format("ui-%s", title.toLowerCase().replaceAll("[\\s_]", "-"));
            return ByUtils.dataTestId("a", dataTestid);
        }
    }

    @Autowired
    private SelectConnectionTypeSteps selectConnectionTypeSteps = new SelectConnectionTypeSteps();

    @Autowired
    private GoogleAccounts googleAccounts;

    @Autowired
    @Lazy
    private CalendarUtils calendarUtils;

    @When("^log out from Syndesis")
    public void logout() {
        $(Element.NAVIGATION_USER_DROPDOWN).shouldBe(visible).click();
        clickOnButton("Logout");

        try {
            OpenShiftWaitUtils.waitFor(() -> WebDriverRunner.getWebDriver().getCurrentUrl().contains("/logout"), 20 * 1000);
        } catch (InterruptedException | TimeoutException e) {
            fail("Log out did not go as expected.");
        }

        TestUtils.sleepForJenkinsDelayIfHigher(3);
        $(Element.LOGIN_BUTTON).shouldBe(visible).click();

        try {
            OpenShiftWaitUtils.waitFor(() -> WebDriverRunner.getWebDriver().getCurrentUrl().contains("login") ||
                $(By.className("pf-c-login")).is(visible), 20 * 1000);
        } catch (InterruptedException | TimeoutException e) {
            fail("Log in page was never loaded");
        }
    }

    @Given("^log into the Syndesis$")
    public void login() {
        doLogin(false);
    }

    @Given("^log into the Syndesis after logout$")
    public void loginAfterLogOut() {
        doLogin(true);
    }

    /**
     * If you want to log in for the first time (browser is not opened) set afterLogout = false
     * If you want to log in again e.g. after logout (browser is already opened) set afterLogout = true
     *
     * @param afterLogout flag to indicate that browser is already open
     */
    private void doLogin(boolean afterLogout) {
        if (!afterLogout) {
            Selenide.open(TestConfiguration.syndesisUrl());
        }

        if ("firefox".equalsIgnoreCase(TestConfiguration.syndesisBrowser())) {
            WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1920, 1080));
        }

        String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

        if (currentUrl.contains(".openshift.com")) {

            //click only if there is Ignite cluster login page
            SelenideElement login = $(By.className("login-redhat"));
            if (login.isDisplayed()) {
                login.click();
            }

            RHDevLogin rhDevLogin = new RHDevLogin();
            rhDevLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        } else if (currentUrl.contains(":8443/login")) {
            log.info("Minishift cluster login page");

            MinishiftLogin minishiftLogin = new MinishiftLogin();
            minishiftLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        } else if (currentUrl.contains("github.com/login")) {

            GitHubLogin gitHubLogin = new GitHubLogin();
            gitHubLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        } else if (currentUrl.contains("oauth/authorize")) {
            String linkText = "htpasswd";
            if (currentUrl.contains("osp")) {
                linkText = "HTPasswd";
            }
            $(By.partialLinkText(linkText)).click();
            MinishiftLogin minishiftLogin = new MinishiftLogin();
            minishiftLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        }

        currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

        if (currentUrl.contains("oauth/authorize/approve")) {
            log.info("Authorize access login page");
            $("input[name=\"approve\"]").shouldBe(visible).click();
        }
    }

    @Given("^created connections$")
    public void createConnections(DataTable connectionsData) {
        Connections connectionsPage = new Connections();

        ConfigureConnectionSteps configureConnectionSteps = new ConfigureConnectionSteps();
        NameConnectionSteps nameConnectionSteps = new NameConnectionSteps();

        List<List<String>> dataTable = connectionsData.cells();

        for (List<String> dataRow : dataTable) {
            String connectionType = validateConnectorName(dataRow.get(0));
            String connectionCredentialsName = dataRow.get(1);
            String connectionName = dataRow.get(2);
            String connectionDescription = dataRow.get(3);

            if ("Gmail".equalsIgnoreCase(connectionType) ||
                "Google Calendar".equalsIgnoreCase(connectionType)) {
                Account a = AccountsDirectory.getInstance().get(connectionCredentialsName);
                GoogleAccount googleAccount = googleAccounts.getGoogleAccountForTestAccount(connectionCredentialsName);
                a.getProperties().put("accessToken", googleAccount.getCredential().getAccessToken());
            }

            navigateTo("Connections");
            validatePage("Connections");

            ElementsCollection connections = connectionsPage.getAllConnections();
            connections = connections.filter(exactText(connectionName));

            try {
                if (connections.size() != 0) {
                    log.warn("Connection {} already exists!", connectionName);
                    continue;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                //this may happen if page was "reloaded" before connections.size was processed, give it second try
                connections = connectionsPage.getAllConnections();
                connections = connections.filter(exactText(connectionName));
                if (connections.size() != 0) {
                    log.warn("Connection {} already exists!", connectionName);
                    continue;
                }
            }

            clickOnLink("Create Connection");

            log.info("Sleeping so jenkins has more time to load all connectors");
            TestUtils.sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay() * 1000);

            selectConnectionTypeSteps.selectConnectionType(connectionType);
            configureConnectionSteps.fillConnectionDetails(connectionCredentialsName);

            // do nothing if connection does not require any credentials
            if (!("no credentials".equalsIgnoreCase(connectionCredentialsName) ||
                "no validation".equalsIgnoreCase(connectionDescription))) {

                clickOnButton("Validate");
                TestUtils
                    .waitFor(() -> $$(Alert.ALL.getBy()).size() > 0,
                        2, 20, "Any notification appears!");
                if (getAllAlerts(connectionType + " does not support validation", "info").size() > 0) {
                    log.warn("Connection type " + connectionType +
                        " doesn't support validation. The test suite assumes that set credentials are correct.");
                } else {
                    successNotificationIsPresentWithError(connectionType + " has been successfully validated", "success");
                }
                scrollTo("top", "right");
                clickOnButton("Next");
            } else if ("no validation".equalsIgnoreCase(connectionDescription)) {
                scrollTo("top", "right");
                clickOnButton("Next");
            }

            nameConnectionSteps.setConnectionName(connectionName);
            nameConnectionSteps.setConnectionDescription(connectionDescription);

            clickOnButton("Save");

            try {
                TestUtils.sleepForJenkinsDelayIfHigher(2);
                OpenShiftWaitUtils.waitFor(() -> !syndesisRootPage.getCurrentUrl().contains("connections/create"), 2, 20);
            } catch (TimeoutException | InterruptedException e) {
                clickOnButton("Save");
                TestUtils.waitFor(() -> !syndesisRootPage.getCurrentUrl().contains("connections/create"),
                    2, 20, "Unable to create a connection - create button does nothing.");
            }
        }
    }

    @Then("^.*validate credentials$")
    public void validateCredentials() {
        Map<String, Account> accounts = AccountsDirectory.getInstance().getAccounts();
        List<List<String>> allAccountsWithDetailsList = new ArrayList<>();

        accounts.keySet().forEach(key -> {
            List<String> accountWithDetailsInList = new ArrayList<>();
            Account currentAccount;
            try {
                currentAccount = AccountsDirectory.getInstance().get(key);
            } catch (IllegalStateException e) {
                return;
            }

            String service = currentAccount.getService();
            Credentials current;
            try {
                current = Credentials.valueOf(service.toUpperCase()
                    .replace(" ", "_")
                    .replace("-", "_")
                    .replaceAll("[()]", ""));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unable to find enum value for " + service + " account." +
                    " New account should be included in smoke tests");
            }

            switch (current) {
                case DROPBOX:
                    service = "DropBox";
                    break;
                case SLACK:
                    service = "Slack";
                    break;
                case TELEGRAM:
                    service = "Telegram";
                    break;
                case SERVICENOW:
                    service = "ServiceNow";
                    break;
                case BOX:
                    service = "Box";
                    break;
                case SEND_EMAIL_SMTP:
                    service = "Send Email (smtp)";
                    break;
                case RECEIVE_EMAIL_IMAP_OR_POP3:
                    service = "Receive Email (imap or pop3)";
                    break;
                default:
                    log.info("Credentials for " + current + " are either tested via OAuth or do not use 3rd party application. Skipping");
                    return;
            }

            //type
            accountWithDetailsInList.add(service);
            //name
            accountWithDetailsInList.add(key);
            //connection name
            accountWithDetailsInList.add("my " + key + " connection");
            //description
            accountWithDetailsInList.add("some description");

            log.trace("Inserting: " + accountWithDetailsInList.toString());
            allAccountsWithDetailsList.add(new ArrayList<>(accountWithDetailsInList));
            log.trace("Current values in list list: " + allAccountsWithDetailsList.toString());
        });

        log.debug("Final status of list: " + allAccountsWithDetailsList.toString());
        DataTable accountsTalbe = DataTable.create(allAccountsWithDetailsList);

        createConnections(accountsTalbe);
    }

    public static final class PageTypeSuggestionProvider implements ISuggestionProvider {

        @Override
        public List<Object> provide(String s) {
            return Arrays.asList("Home", "Integrations", "Connections", "Customizations", "Settings", "Support");
        }
    }

    @When("^navigate to the \"([^\"]*)\" page$")
    public void navigateTo(@Suggestion(PageTypeSuggestionProvider.class) String title) {
        try {
            OpenShiftWaitUtils.waitFor(() -> $(Element.NAVIGATION_PANEL).exists(), 30 * 1000L);
        } catch (TimeoutException | InterruptedException e) {
            fail("Navigation panel was not found in 30s", e);
        }

        $(Element.NAVIGATION_PANEL).shouldBe(visible);
        $(Element.navigationLink(title)).shouldBe(visible).click();
    }

    @When("^.*navigates? to the \"([^\"]*)\" page in help menu$")
    public void navigateToHelp(String title) {
        SelenideElement helpDropdownMenu = $(Element.HELP_DROPDOWN_BUTTON).shouldBe(visible);

        //open the help menu
        if (helpDropdownMenu.parent().$$(Element.DROPDOWN_MENU).size() < 1) {
            helpDropdownMenu.click();
        }

        SelenideElement dropdownElementsTable = $(Element.DROPDOWN_MENU).shouldBe(visible);
        ElementsCollection dropdownElements = dropdownElementsTable.findAll(By.tagName("a"))
            .shouldBe(CollectionCondition.sizeGreaterThanOrEqual(1));

        dropdownElements.filter(text(title)).shouldHaveSize(1).get(0).shouldBe(visible).click();

        //TODO: following if statement can be removed after
        //TODO: this issue gets fixed: https://github.com/syndesisio/syndesis/issues/4655
        //close the help menu
        if (helpDropdownMenu.parent().$$(Element.DROPDOWN_MENU).size() >= 1) {
            helpDropdownMenu.click();
        }
    }

    @Then("^check visibility of Syndesis home page$")
    public void checkHomePageVisibility() {
        syndesisRootPage.getRootElement().shouldBe(visible);
    }

    @Then("^check \"([^\"]*)\" link is (not |)visible$")
    public void validateLink(String linkTitle, String visibility) {
        Condition condition = "not".equalsIgnoreCase(visibility.trim()) ? hidden : visible;
        new SyndesisRootPage().getLink(linkTitle).shouldBe(condition);
    }

    public static final class ButtonNameProvider implements ISuggestionProvider {
        @Override
        public List<Object> provide(String s) {
            return Selenide.$$(By.tagName("button")).stream().map(SelenideElement::text).collect(Collectors.toList());
        }
    }

    @When("^click? on the \"([^\"]*)\" button.*$")
    public void clickOnButton(@Suggestion(ButtonNameProvider.class) String buttonTitle) {
        UIUtils.ensureUILoaded();
        if ("Done".equals(buttonTitle)) {
            // this is hack to replace Done with Next if not present
            try {
                syndesisRootPage.getRootElement().shouldBe(visible).findAll(By.tagName("button"))
                    .filter(matchText("(\\s*)" + buttonTitle + "(\\s*)")).first().waitUntil(visible, 10 * 1000);
            } catch (Throwable t) {
                buttonTitle = "Next";
            }
        }
        SelenideElement button = syndesisRootPage.getButton(buttonTitle);
        log.info(button.toString());
        button.shouldBe(visible, enabled).shouldNotHave(attribute("disabled")).click();
    }

    public void clickOnButtonByCssClassName(String buttonCssClass) {
        SelenideElement button = syndesisRootPage.getButtonByCssClassName(buttonCssClass);
        button.shouldBe(visible, enabled).shouldNotHave(attribute("disabled")).click();
    }

    @When(".*clicks? on the modal dialog \"([^\"]*)\" button.*$")
    public void clickOnModalDialogButton(String buttonTitle) {
        modalDialogPage.getButton(buttonTitle).shouldBe(visible).click();
    }

    @When("cancel modal dialog window$")
    public void cancelModalDialogWindow() {
        if (modalDialogPage.validate()) {
            modalDialogPage.getButton("Cancel").shouldBe(visible).click();
        }

        try {
            OpenShiftWaitUtils.waitFor(() -> !modalDialogPage.validate(), 15 * 1000L);
        } catch (InterruptedException | TimeoutException e) {
            fail("Modal dialog is still visible after clicking on cancel");
        }
    }

    public static final class LinkNameProvider implements ISuggestionProvider {
        @Override
        public List<Object> provide(String s) {
            return Selenide.$$("a").stream().map(SelenideElement::text).collect(Collectors.toList());
        }
    }

    @When(".*clicks? on the \"([^\"]*)\" link.*$")
    public void clickOnLink(@Suggestion(LinkNameProvider.class) String linkTitle) {
        if ("Customizations".equals(linkTitle) &&
            $(ByUtils.dataTestId("ui-api-client-connectors")).isDisplayed()) {
            //do not click when customizations menu is already visible
            return;
        }
        new SyndesisRootPage().getLink(linkTitle).shouldBe(visible).click();
    }

    @When(".*click on element with data-testid \"([^\"]*)\"$")
    public void clickOnElement(String element) {
        $(ByUtils.dataTestId(element)).shouldBe(visible).click();
    }

    @When(".*click on element with id \"([^\"]*)\"$")
    public void clickOnElementId(String id) {
        $(By.id(id)).shouldBe(visible).click();
    }

    @When(".*clicks? on the \"(\\d+)\". \"([^\"]*)\" link.*$")
    public void clickOnLink(int position, String linkTitle) {
        int index = position - 1; // e.g. first position is 0 index
        new SyndesisRootPage().getLink(linkTitle, index).shouldBe(visible).click();
    }

    @When(".*clicks? on the random \"([^\"]*)\" link.*$")
    public void clickOnLinkRandom(String linkTitle) {
        new SyndesisRootPage().getLinkRandom(linkTitle);
    }

    @Then("^check visibility of the \"([^\"]*)\" button$")
    public void checkButtonIsVisible(String buttonTitle) {
        new SyndesisRootPage().getButton(buttonTitle).shouldBe(visible);
    }

    @Then("^check visibility of the \"([^\"]*)\" tables$")
    public void checkTableTitlesArePresent(String tableTitles) {

        String[] titles = tableTitles.split(",");

        for (String title : titles) {
            new SyndesisRootPage().getTitleByText(title).shouldBe(visible);
        }
    }

    public void expectTableTitlePresent(String tableTitle) {
        SelenideElement table = new SyndesisRootPage().getTitleByText(tableTitle);
        table.shouldBe(visible);
    }

    @Then("^check visibility of the \"([^\"]*)\" elements$")
    public void expectElementsPresent(String elementClassNames) {
        String[] elementClassNamesArray = elementClassNames.split(",");

        for (String elementClassName : elementClassNamesArray) {
            SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
            element.shouldBe(visible);
        }
    }

    @Then("^check visibility of the \"([^\"]*)\"$")
    public void expectElementPresent(String elementClassName) {
        SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
        element.shouldBe(visible);
    }

    @Then("^check visibility of success notification$")
    public void successNotificationIsPresent() {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByLocator(Alert.SUCCESS.getBy());
        allertSucces.shouldBe(visible);
    }

    @Then("^check visibility of \"([^\"]*)\" in alert-(\\w+) notification$")
    public void successNotificationIsPresentWithError(String textMessage, String type) {
        TestUtils
            .waitFor(() -> $$(Alert.getALERTS().get(type).getBy()).filterBy(Condition.matchesText(sanitizeSpecialCharacter(textMessage))).size() == 1,
                2, 20, "Success notification not found!");
        ElementsCollection successList = getAllAlerts(textMessage, type);
        assertThat(successList).hasSize(1);
        log.info("Text message {} was found.", textMessage);
    }

    private ElementsCollection getAllAlerts(String textMessage, String type) {
        return $$(Alert.getALERTS().get(type).getBy()).filterBy(Condition.matchesText(sanitizeSpecialCharacter(textMessage)));
    }

    /**
     * When a text message contains special characters, they needs to be sanitize because the Condition.matchesText use the text message as a regex
     * pattern
     * e.g.
     * "Send Email (smtp) has been successfully validated"
     * "Send Email \(smtp\) has been successfully validated"
     */
    private String sanitizeSpecialCharacter(String textMessage) {
        String result = textMessage.replaceAll("\\(", "\\\\(");
        result = result.replaceAll("\\)", "\\\\)");
        return result;
    }

    @Then("^check visibility of alert notification$")
    public void checkSqlWarning() {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByLocator(Alert.WARNING.getBy());
        allertSucces.shouldBe(visible);
    }

    /**
     * Scroll the webpage.
     *
     * @param topBottom possible values: top, bottom
     * @param leftRight possible values: left, right
     */
    @When("^scroll \"([^\"]*)\" \"([^\"]*)\"$")
    public void scrollTo(String topBottom, String leftRight) {
        WebDriver driver = WebDriverRunner.getWebDriver();
        JavascriptExecutor jse = (JavascriptExecutor) driver;

        int x = 0;
        int y = 0;

        Long width = (Long) jse.executeScript("return document.documentElement.scrollWidth");
        Long height = (Long) jse.executeScript("return document.documentElement.scrollHeight");

        if ("right".equals(leftRight)) {
            y = width.intValue();
        }

        if ("bottom".equals(topBottom)) {
            x = height.intValue();
        }

        jse.executeScript("(browserX, browserY) => window.scrollTo(browserX, browserY)", x, y);
    }

    @Then("^check visibility of page \"([^\"]*)\"$")
    public void validatePage(String pageName) {
        SyndesisPage.get(pageName).validate();
    }

    @When("^select \"([^\"]*)\" from \"([^\"]*)\" dropdown$")
    public void selectsFromDropdown(String option, String selectDataTestid) {
        //search by name or by data-testid because some dropdowns have only id
        SelenideElement selectElement = $(ByUtils.dataTestId("select", selectDataTestid))
            .shouldBe(visible);
        selectElement.selectOption(option);
    }

    @Then("^sleep for \"(\\w+)\" ms$")
    public void sleep(Integer ms) {
        Selenide.sleep(ms);
    }

    @Then("^check \"([^\"]*)\" button is \"([^\"]*)\"$")
    public void sheCheckButtonStatus(String buttonTitle, String status) {
        new SyndesisRootPage().checkButtonStatus(buttonTitle, status);
    }

    @Then("^check visibility of dialog page \"(.*)\"$")
    public void isPresentedWithDialogPage(String title) {
        String titleText = new ModalDialogPage().getTitleText();
        assertThat(titleText).isEqualToIgnoringCase(title);
    }

    @Then("^removes? file \"([^\"]*)\" if it exists$")
    public void removeFileIfExists(String fileName) throws Throwable {
        Files.deleteIfExists(Paths.get(CustomWebDriverProvider.DOWNLOAD_DIR + File.separator + fileName));
    }

    /**
     * When connector name is changed in the product just create new case here instead of changing it in the whole
     * testsuite string by string.
     *
     * @param name connector name
     * @return connector name
     */
    private String validateConnectorName(String name) {
        String finalName = name;
        switch (name) {
            case "DropBox":
                finalName = "Dropbox";
                break;
        }
        return finalName;
    }

    /**
     * This is only general form step that may work in most cases but it's better to use specific form-filling steps for each page
     *
     * @param data data
     */
    @Then("^fills? in values$")
    public void fillForm(DataTable data) {
        new Form(new SyndesisRootPage().getRootElement()).fillByLabel(data.asMap(String.class, String.class));
    }

    @Then("^fill in values by element ID")
    public void fillFormViaID(DataTable data) {
        Form.waitForInputs(20);
        TestUtils.sleepIgnoreInterrupt(2000);
        new Form(new SyndesisRootPage().getRootElement()).fillById(data.asMap(String.class, String.class));
    }

    @Then("^fill in values by element data-testid")
    public void fillFormViaTestID(DataTable data) {
        Form.waitForInputs(20);
        TestUtils.sleepIgnoreInterrupt(2000);
        Map<String, String> dataMap = new HashMap<>(data.asMap(String.class, String.class));
        dataMap.replaceAll((key, value) -> value == null ? "" : value);
        new Form(new SyndesisRootPage().getRootElement()).fillByTestId(dataMap);
    }

    @Then("^fill in data-testid field \"([^\"]*)\" from property \"([^\"]*)\" of credentials \"([^\"]*)\"")
    public void fillFormByTestIdFromCreds(String testId, String property, String credentials) {
        Account account = AccountsDirectory.getInstance().get(credentials);
        Map<String, String> map = new HashMap<>();
        map.put(testId, account.getProperty(property));

        new Form(new SyndesisRootPage().getRootElement()).fillByTestId(map);
    }

    @Then("^force fill in values by element data-testid$")
    public void forceFillFormViaTestID(DataTable data) {
        Form.waitForInputs(20);
        TestUtils.sleepIgnoreInterrupt(2000);
        new Form(new SyndesisRootPage().getRootElement()).forceFillByTestId(data.asMap(String.class, String.class));
    }

    // for CodeMirror text editor
    @When("^fill text into text-editor$")
    public void fillTextIntoTextEditor(DataTable data) {
        StringBuilder text = new StringBuilder();
        data.asList().forEach(s -> {
            text.append(s);
        });

        new Form(new SyndesisRootPage().getRootElement()).fillEditor(text.toString());
    }

    @When("^.*create connections using oauth$")
    public void createConnectionsUsingOAuth(DataTable connectionsData) {

        List<List<String>> dataTable = connectionsData.cells();

        for (List<String> dataRow : dataTable) {
            createConnectionUsingOAuth(dataRow.get(0), dataRow.get(1));
        }
    }

    @When("^.*create connection \"([^\"]*)\" with name \"([^\"]*)\" using oauth$")
    public void createConnectionUsingOAuth(String connectorName, String newConnectionName) {
        Connections connectionsPage = new Connections();
        NameConnectionSteps nameConnectionSteps = new NameConnectionSteps();

        navigateTo("Connections");
        validatePage("Connections");

        ElementsCollection connections = connectionsPage.getAllConnections();
        connections = connections.filter(exactText(newConnectionName));

        assertThat(connections.size()).as("Connection with name " + newConnectionName + " already exists!")
            .isEqualTo(0);

        clickOnLink("Create Connection");

        //sometimes page is loaded but connections are not so we need to wait here a bit
        TestUtils.sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay());

        selectConnectionTypeSteps.selectConnectionType(connectorName);

        //slenide did validation before it reached correct page, but lets wait a second (it helps, trust me!)
        TestUtils.sleepForJenkinsDelayIfHigher(1);
        doOAuthValidation(connectorName);

        assertThat(WebDriverRunner.currentFrameUrl())
            .containsIgnoringCase("review")
            .containsIgnoringCase("connections/create");

        nameConnectionSteps.setConnectionName(newConnectionName);

        try {
            TestUtils.sleepForJenkinsDelayIfHigher(2);
            OpenShiftWaitUtils.waitFor(() -> !syndesisRootPage.getCurrentUrl().contains("connections/create"), 2, 20);
        } catch (TimeoutException | InterruptedException e) {
            clickOnButton("Save");
            TestUtils.waitFor(() -> !syndesisRootPage.getCurrentUrl().contains("connections/create"),
                2, 20, "Unable to create a connection - create button does nothing.");
        }
    }

    @When("^go back in browser history$")
    public void clickBrowserBackButton() {
        Selenide.back();
    }

    private void waitForAdditionalWindow() {
        TestUtils.waitFor(() -> WebDriverRunner.getWebDriver().getWindowHandles().size() > 1,
            2, 60,
            "Second window has not shown up");
    }

    private void waitForWindowToShowUrl() {
        TestUtils.waitFor(() -> {
            try {
                WebDriverRunner.getWebDriver().getCurrentUrl();
            } catch (WebDriverException e) {
                return false;
            }

            return true;
        }, 1, 20, "Window has not returned valid url");
    }

    @ExcludeFromSelectorReports
    private void doOAuthValidation(String type) {

        WebDriver driver = WebDriverRunner.getWebDriver();

        // store the current window handle
        String winHandleBefore = driver.getWindowHandle();
        log.info("Current window handle identifier: " + winHandleBefore);

        // close additional browser windows
        if (driver.getWindowHandles().size() > 1) {
            log.error("There is more than one window opened!");
            closeAdditionalBrowserWindows(winHandleBefore);
        }

        // Perform the click operation that opens new window
        clickOnButton("Connect " + type);

        // we need to wait for an oauth window to open after button click
        waitForAdditionalWindow();

        // Switch to new window opened
        for (String winHandle : driver.getWindowHandles()) {
            if (winHandle.equalsIgnoreCase(winHandleBefore)) {
                continue;
            }
            log.info("Found another windows handle: " + winHandle);
            driver.switchTo().window(winHandle);
        }

        // need to wait a bit until window is able to return url
        waitForWindowToShowUrl();

        if ("firefox".equalsIgnoreCase(TestConfiguration.syndesisBrowser())) {
            WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1920, 1080));
        }

        // Perform the actions on new window

        switch (type) {
            case "Twitter":
                waitForCallbackRedirect("twitter");
                fillAndValidateTwitter();
                break;
            case "Salesforce":
                waitForCallbackRedirect("salesforce");
                fillAndValidateSalesforce();
                break;
            case "Gmail":
                loginToGoogleIfNeeded("QE Google Mail");
                break;
            case "Google Calendar":
                loginToGoogleIfNeeded("QE Google Calendar");
                break;
            case "SAP Concur":
                waitForCallbackRedirect("concursolutions");
                fillAndValidateConcur();
                break;
            case "Google Sheets":
                loginToGoogleIfNeeded("QE Google Sheets");
                break;

            default:
                fail("Unknown oauth option: " + type);
        }

        // Switch back to original browser (first window)
        driver.switchTo().window(winHandleBefore);

        waitForCallbackRedirect("review");

        // close hanging windows
        closeAdditionalBrowserWindows(winHandleBefore);

        if ("firefox".equalsIgnoreCase(TestConfiguration.syndesisBrowser())) {
            WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1920, 1080));
        }
    }

    private void closeAdditionalBrowserWindows(String currentWindow) {
        for (String winHandle : WebDriverRunner.getWebDriver().getWindowHandles()) {
            if (winHandle.equalsIgnoreCase(currentWindow)) {
                continue;
            }
            log.warn("Found another browser window handle and closing: " + winHandle);
            WebDriverRunner.getWebDriver().switchTo().window(winHandle);
            WebDriverRunner.getWebDriver().close();
        }
        WebDriverRunner.getWebDriver().switchTo().window(currentWindow);
    }

    private void loginToGoogleIfNeeded(String s) {
        // if the browser has previously logged into google account syndesis will
        // immediately move to next screen and will have "Successfully%20authorized%20Syndesis's%20access" in the URL
        log.info("Current url: {}", WebDriverRunner.getWebDriver().getCurrentUrl().toLowerCase());

        if (isStringInUrl("Successfully%20authorized%20Syndesis's%20access", 5)
            || $(Alert.SUCCESS.getBy()).is(visible)) {

            log.info("User is already logged");
            return;
        }

        waitForCallbackRedirect("google");
        fillAndValidateGoogleAccount(s);
    }

    private void fillAndValidateTwitter() {
        Account account = AccountsDirectory.getInstance().get(Account.Name.TWITTER_LISTENER);

        try {
            OpenShiftWaitUtils.waitFor(() -> $(By.id("username_or_email")).exists(), 10 * 1000L);
        } catch (InterruptedException | TimeoutException e) {
            log.info("Already logged into Twitter, clicking on validate");
            $(By.id("allow")).click();
            return;
        }

        $(By.id("username_or_email")).shouldBe(visible).sendKeys(account.getProperty("login"));
        $(By.id("password")).shouldBe(visible).sendKeys(account.getProperty("password"));
        $(By.id("allow")).shouldBe(visible).click();
    }

    private void fillAndValidateSalesforce() {
        Account account = AccountsDirectory.getInstance().get(Account.Name.SALESFORCE);

        if (isStringInUrl("%22message%22:%22Successfully%20authorized", 5)) {
            log.info("Salesforce is already connected");
            return;
        }

        //Clicking on validate takes you to step 3.
        //So instead if there's an alert saying you are already connected we skip clicking the validate button
        if ($(By.cssSelector("alert")).exists()) {
            log.info("Found alert, probably about account already being authorized");
            log.info("{}", $(By.className("alert")).getText());
            return;
        }

        $(By.id("username")).shouldBe(visible).sendKeys(account.getProperty("userName"));
        $(By.id("password")).shouldBe(visible).sendKeys(account.getProperty("password"));
        $(By.id("Login")).shouldBe(visible).click();
        //give it time to log in
        TestUtils.sleepForJenkinsDelayIfHigher(10);
    }

    /**
     * This is a special method for this page https://accounts.google.com/accountchooser
     * The last option leads to normal login window
     */
    private void selectGoogleAccountIfNeeded() {
        if ($(By.cssSelector(".y77zqe")).exists()) {
            $$(By.cssSelector(".OVnw0d")).last().click();
        }
    }

    private void fillAndValidateGoogleAccount(String googleAccount) {
        Account account = AccountsDirectory.getInstance().get(googleAccount);

        selectGoogleAccountIfNeeded();

        $(By.id("identifierId")).shouldBe(visible).sendKeys(account.getProperty("email"));
        $(By.id("identifierNext")).shouldBe(visible).click();

        $(By.id("password")).shouldBe(visible).find(By.tagName("input")).sendKeys(account.getProperty("password"));
        $(By.id("passwordNext")).shouldBe(visible).click();
    }

    private void fillAndValidateConcur() {
        Account account = AccountsDirectory.getInstance().get(Account.Name.CONCUR);

        $$(By.tagName("input")).stream()
            .filter(e ->
                e.getAttribute("name").equalsIgnoreCase("type") &&
                    e.getAttribute("value").equalsIgnoreCase("username"))
            .findFirst().get().click();
        $(By.id("userid")).shouldBe(visible).sendKeys(account.getProperty("userId"));
        $(By.xpath(".//*[@type='submit']")).shouldBe(visible).click();
        TestUtils.sleepForJenkinsDelayIfHigher(3);
        $(By.id("password")).shouldBe(visible).sendKeys(account.getProperty("password"));
        $(By.xpath(".//*[@type='submit']")).shouldBe(visible).click();
    }

    private void waitForCallbackRedirect(String expectedPartOfUrl) {
        waitForCallbackRedirect(expectedPartOfUrl, 60);
    }

    private void waitForCallbackRedirect(String expectedPartOfUrl, int timeoutSeconds) {
        try {
            waitFor(() -> WebDriverRunner.currentFrameUrl().contains(expectedPartOfUrl.toLowerCase()), timeoutSeconds * 1000);
        } catch (InterruptedException | TimeoutException e) {
            assertThat(WebDriverRunner.currentFrameUrl())
                .as("Error while redirecting to " + expectedPartOfUrl)
                .containsIgnoringCase(expectedPartOfUrl);
        }
    }

    private void waitForStringInUrl(String expectedPartOfUrl, int timeoutSeconds) throws TimeoutException, InterruptedException {
        waitFor(() -> {
            String frameUrl = WebDriverRunner.getWebDriver().getCurrentUrl().toLowerCase();
            return frameUrl.contains(expectedPartOfUrl.toLowerCase());
        }, timeoutSeconds * 1000);
    }

    private boolean isStringInUrl(String toFind, int timeoutSeconds) {
        boolean result = false;
        try {
            waitForStringInUrl(toFind, timeoutSeconds);
            result = true;
        } catch (InterruptedException | TimeoutException e) {
            log.debug("error", e);
        }
        return result;
    }

    @When("^enable 3scale discovery with url \\\"([^\\\"]*)\\\"")
    public void enable3scaleEnvVar(String value) {
        set3scaleEnvVar(value);
    }

    @When("^disable 3scale discovery")
    public void disable3scaleEnvVar() {
        set3scaleEnvVar(null);
    }

    /**
     * Enable or disable 3scale discovery of an API provided by Syndesis
     *
     * @param url URL of 3scale user interface or NULL for turning discovery off
     */
    private void set3scaleEnvVar(String url) {
        Syndesis syndesis = ResourceFactory.get(Syndesis.class);
        JSONObject cr = new JSONObject(syndesis.getCr());
        JSONObject features = cr.getJSONObject("spec").getJSONObject("components").getJSONObject("server").getJSONObject("features");

        if (url != null) {
            features.put("managementUrlFor3scale", url);
        } else {
            features.remove("managementUrlFor3scale");
        }

        syndesis.editCr(cr.toMap());

        try {
            OpenShiftWaitUtils.waitForPodIsReloaded("server");
        } catch (InterruptedException | TimeoutException e) {
            fail("Server was not reloaded after deployment config change", e);
        }
        // even though server is in ready state, inside app is still starting so we have to wait a lot just to be sure
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getPodLogs("server").contains("Started Application in"), 1000 * 300L);
        } catch (TimeoutException | InterruptedException e) {
            fail("Syndesis server did not start in 300s with new variable", e);
        }

        PortForwardUtils.createOrCheckPortForward();
        Selenide.refresh();
    }

    @Then("^check that 3scale annotations are present on integration \"([^\"]*)\"")
    public void check3scaleAnnotations(String integrationName) {
        Map<String, String> annotations =
            OpenShiftUtils.getInstance().getService(("i-" + integrationName).toLowerCase()).getMetadata().getAnnotations();

        assertThat(annotations)
            .contains(entry("discovery.3scale.net/description-path", "/openapi.json"))
            .contains(entry("discovery.3scale.net/port", "8080"))
            .contains(entry("discovery.3scale.net/scheme", "http"));
    }

    //TODO: should be refactored after asmigala is done with api provider tests
    @When("^select first api provider operation$")
    public void clickFirstOperation() {
        TestUtils.waitFor(() -> $$(By.className("list-pf-title")).size() >= 1,
            2, 30, "Api provider operations were not loaded in 30s");
        $$(By.className("list-pf-title")).first().shouldBe(visible).click();
    }

    /**
     * Save current time to the singleton class
     */
    @Then("^save time before request for integration ([^\"]*)$")
    public void saveBeforeTime(String integrationName) {
        calendarUtils.setBeforeRequest(Calendar.getInstance(), integrationName);
        log.info("Time before request was saved: "
            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendarUtils.getLastBeforeRequest().getTime()));
        TestUtils.sleepIgnoreInterrupt(3000); // due to border values
    }

    /**
     * Save current time to the singleton class
     */
    @When("^save time after request for integration ([^\"]*)$")
    public void saveAfterTime(String integrationName) {
        TestUtils.sleepIgnoreInterrupt(3000); // due to border values
        calendarUtils.setAfterRequest(Calendar.getInstance(), integrationName);
        log.info("Time after request was saved: "
            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendarUtils.getLastAfterRequest().getTime()));
    }

    @When("^clean webdriver download folder$")
    public void cleanDownloadFolder() throws IOException {
        CustomWebDriverProvider.cleanDownloadFolder();
    }

    @Then("^check that main alert dialog contains text \"([^\"]*)\"$")
    public void checkAlertDialog(String expectedText) {
        assertThat(syndesisRootPage.getDangerAlertElemet().getText()).contains(expectedText);
    }
}
