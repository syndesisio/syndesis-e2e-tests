package io.syndesis.qe.pages;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SyndesisPageObject {

	public abstract SelenideElement getRootElement();

	public abstract boolean validate();

	public SelenideElement getButton(String buttonTitle) {
		log.info("searching for button {}", buttonTitle);
		return this.getRootElement().find(By.xpath(String.format("//button[contains(text(), '%s')]", buttonTitle)));
	}

	public void clickOnFirstVisibleButton(String buttonTitle) {
		log.info("searching for first visible button {}", buttonTitle);
		ElementsCollection buttonElements = this.getRootElement().findAll(By.linkText(buttonTitle)).filter(visible);
		buttonElements.get(0).click();
	}

	public void clickButton(String buttonTitle) {
		log.info("clicking button {}", buttonTitle);
		SelenideElement buttonElement = this.getButton(buttonTitle).shouldBe(visible);
		buttonElement.click();
	}

	/**
	 * Fill form with given data. It will look for ui element for every map entry.
	 *
	 * @param data key,value data. Key is used for element lookup
	 * @param parrentElement search inputs in child elements of this one
	 */
	public void fillForm(Map<By, String> data, SelenideElement parrentElement) {
		String value;
		for (By locator : data.keySet()) {
			value = data.get(locator);
			SelenideElement inputElement = parrentElement.find(locator).shouldBe(visible);
			inputElement.sendKeys(value);
		}
	}

	/*
	* @param select Combobox select element.
	* @param optionNumber number of the option from list of options.
	*/
	public void selectFromDropDown(SelenideElement select, String option) {
		SelenideElement selectGroup = select.find(By.xpath("'.."));
		SelenideElement dropDownToggle = selectGroup.find(By.className("dropdown-toggle"));
		dropDownToggle.click();
		dropDownToggle.click();
		SelenideElement selectOption = selectGroup.find(By.cssSelector(String.format("li[data-value=\"%s\"]"))).shouldBe(visible);
		selectOption.click();
	}

	public String getCurrentUrl() {
		return getWebDriver().getCurrentUrl();
	}

	public void goToUrl(String url) {
		getWebDriver().get(url);
	}

	public SelenideElement getLink(String linkTitle) {
		return this.getRootElement().find(By.linkText(linkTitle));
	}

	public ElementsCollection getLinks(String linkTitle) {
		return this.getRootElement().findAll(By.linkText(linkTitle));
	}

	public void clickLink(String linkTitle) {
		this.getLink(linkTitle).shouldBe(visible).click();
	}

	public void clickLinkRandom(String linkTitle) {
		ElementsCollection links = this.getLinks(linkTitle);
		int index = (int) Math.floor(Math.random() * links.size());
		links.get(index).shouldBe(visible).click();
	}

	public SelenideElement getElementByCssSelector(String cssSelector) {
		return this.getRootElement().find(By.cssSelector(cssSelector));
	}

	public SelenideElement getElementByXpath(String xpathSelector) {
		return this.getRootElement().find(By.xpath(xpathSelector));
	}

	public SelenideElement getElementByClassName(String elementClassName) {
		return this.getRootElement().find(By.className(elementClassName));
	}

	public SelenideElement getElementByLocator(By elementLocator) {
		log.info("searching for element {}", elementLocator.toString());
		return this.getRootElement().$(elementLocator);
	}

	SelenideElement getTitleByText(String text){
		log.info("searching for title {}", text);
		return this.getRootElement().find(By.cssSelector(String.format("h2:contains('%s')", text)));
	}


	public ElementsCollection getElementsByClassName(String elementClassName) {
		return this.getRootElement().findAll(By.className(elementClassName));
	}

	public void clickElementRandom(String elementClassName) {
		ElementsCollection elements = this.getElementsByClassName(elementClassName);
		int index = (int) Math.floor(Math.random() * elements.size());
		elements.get(index).shouldBe(visible).click();
	}

	public void selectOption(SelenideElement selectElement, String option) {
		SelenideElement optionElement = getElementContainingText(By.tagName("option"), option);
		optionElement.shouldBe(visible).click();
	}
	
	public SelenideElement getInputById(String inputId) {
		return this.getRootElement().find(By.id(inputId));
	}

	public SelenideElement getInputBySelector(String selector) {
		return this.getRootElement().find(By.cssSelector(selector));
	}

	public void fillInput(String inputId, String value) {
		SelenideElement input = this.getInputById(inputId);
		input.shouldBe(visible).clear();
		input.shouldBe(visible).sendKeys(value);
	}

	public void fillInput(SelenideElement element, String value) {
		element.shouldBe(visible).clear();
		element.shouldBe(visible).sendKeys(value);
	}

	public SelenideElement getElementContainingText(By by, String text) {
		ElementsCollection elements = getRootElement().findAll(by);
		elements = elements.filter(exactText(text));
		SelenideElement element = elements.shouldBe(sizeGreaterThan(0)).first();
		return element;
	}
}
