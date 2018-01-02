package io.syndesis.qe.steps;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.pages.SyndesisPage;
import io.syndesis.qe.pages.SyndesisRootPage;
import io.syndesis.qe.pages.login.GitHubLogin;
import io.syndesis.qe.pages.login.MinishiftLogin;
import io.syndesis.qe.pages.login.RHDevLogin;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonSteps {

	private SyndesisRootPage syndesisRootPage = new SyndesisRootPage();

	@Given("^\"([^\"]*)\" logs into the Syndesis$")
	public void login(String username) throws Throwable {
		Selenide.open(TestConfiguration.syndesisUrl());

		if (!WebDriverRunner.isChrome()) {
			WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1920, 1024));
		}
		
		String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

		if (currentUrl.contains("api.fuse-ignite.openshift.com")) {

			//click only if there is Ignite cluster login page
			SelenideElement login = $(By.className("login-redhat"));
			if(login.isDisplayed()) {
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
		}

		currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

		if (currentUrl.contains("oauth/authorize/approve")) {
			log.info("Authorize access login page");
			$("input[name=\"approve\"]").shouldBe(visible).click();
		}
	}

	@When("^\"([^\"]*)\" navigates? to the \"([^\"]*)\" page$")
	public void navigateTo(String username, String title) {
		SelenideElement selenideElement = $(By.className("nav-pf-vertical")).shouldBe(visible);
		ElementsCollection allLinks = selenideElement.findAll(By.className("list-group-item-value"));
		allLinks.find(Condition.exactText(title)).shouldBe(visible).click();
	}

	@Given("^clean application state$")
	public void resetState() {
		Long result = (Long) ((JavascriptExecutor) WebDriverRunner.getWebDriver())
				.executeAsyncScript("var callback = arguments[arguments.length - 1]; " +
						"$.get('/api/v1/test-support/reset-db', function(data, textStatus, jqXHR) { callback(jqXHR.status); })");

		Assertions.assertThat(String.valueOf(result)).isEqualTo("204");
	}

	@Then("^\"(\\w+)\" is presented with the Syndesis home page$")
	public void checkHomePageVisibility(String username) {
		syndesisRootPage.getRootElement().shouldBe(visible);
	}

	@Then("^(\\w+)? is presented with the \"([^\"]*)\" link$")
	public void validateLink(String alias, String linkTitle) {
		new SyndesisRootPage().getLink(linkTitle).shouldBe(visible);
	}

	@When(".*clicks? on the \"([^\"]*)\" button.*$")
	public void clickOnButton(String buttonTitle) { new SyndesisRootPage().clickButton(buttonTitle); }

	@When(".*clicks? on the \"([^\"]*)\" link.*$")
	public void clickOnLink(String linkTitle) {
		new SyndesisRootPage().clickLink(linkTitle);
	}

	@When(".*clicks? on the random \"([^\"]*)\" link.*$")
	public void clickOnLinkRandom(String linkTitle) {
		new SyndesisRootPage().clickLinkRandom(linkTitle);
	}

	@Then("^she is presented with the \"([^\"]*)\" button$")
	public void checkButtonIsVisible(String buttonTitle) {
		new SyndesisRootPage().getButton(buttonTitle).shouldBe(visible);
	}

	@Then("^she is presented with the \"([^\"]*)\" tables$")
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

	@Then("^she is presented with the \"([^\"]*)\" elements$")
	public void expectElementsPresent(String elementClassNames) {
		String[] elementClassNamesArray = elementClassNames.split(",");

		for (String elementClassName : elementClassNamesArray) {
			SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
			element.shouldBe(visible);
		}
	}

	@Then("^she is presented with the \"([^\"]*)\"$")
	public void expectElementPresent(String elementClassName) {
		SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
		element.shouldBe(visible);
	}

	@Then("^she can see success notification$")
	public void successNotificationIsPresent() {
		SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-success");
		allertSucces.shouldBe(visible);
	}

	@Then("^she can see \"([^\"]*)\" in alert-success notification$")
	public void successNotificationIsPresentWithError(String textMessage) {
		SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-success");
		allertSucces.shouldBe(visible);
		Assertions.assertThat(allertSucces.getText().equals(textMessage)).isTrue();

		log.info("Text message {} was found.", textMessage);
	  }

	/**
	 * Scroll the webpage.
	 *
	 * @param topBottom possible values: top, bottom
	 * @param leftRight possible values: left, right
	 * @returns {Promise<any>}
	 */
	@When("^scroll \"([^\"]*)\" \"([^\"]*)\"$")
	public void scrollTo(String topBottom, String leftRight) {
		WebDriver driver = WebDriverRunner.getWebDriver();
		JavascriptExecutor jse = (JavascriptExecutor) driver;

		int x = 0;
		int y = 0;

		Long width = (Long) jse.executeScript("return $(document).width()");
		Long height = (Long) jse.executeScript("return $(document).height()");

		if (leftRight.equals("right")) {
			y = width.intValue();
		}

		if (topBottom.equals("bottom")) {
			x = height.intValue();
		}

		jse.executeScript("(browserX, browserY) => window.scrollTo(browserX, browserY)", x, y);
	}

	@Then("^(\\w+) is presented with the Syndesis page \"([^\"]*)\"$")
	public void validatePage(String userName, String pageName) {
		SyndesisPage.get(pageName).validate();
	}

	@And("^she selects \"([^\"]*)\" from \"([^\"]*)\" dropdown$")
	public void selectsFromDropdown(String option, String selectId) throws Throwable {
		SelenideElement selectElement = $(String.format("select[id=\"%s\"]", selectId)).shouldBe(visible);
		selectElement.selectOption(option);
	}

	@Then("^she stays there for \"(\\w+)\" ms$")
	public void sleep(Integer ms) {
		Selenide.sleep(ms);
	}

	@Then("^she checks \"([^\"]*)\" button is \"([^\"]*)\"$")
	public void sheCheckButtonStatus(String buttonTitle, String status) throws Throwable {
		new SyndesisRootPage().checkButtonStatus(buttonTitle, status);
	}

}
