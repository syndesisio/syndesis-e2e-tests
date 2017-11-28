package io.syndesis.qe.pages.integrations.edit;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.openqa.selenium.By;

import com.codeborne.selenide.SelenideElement;

import io.syndesis.qe.pages.SyndesisPageObject;
import io.syndesis.qe.pages.connections.list.ConnectionsListComponent;

public class IntegrationConnectionSelectComponent extends SyndesisPageObject {
	
	private static final class Element {
		public static final By ROOT = By.cssSelector("syndesis-integrations-connection-select");
	}

	public SelenideElement getRootElement() {
		SelenideElement elementRoot = $(Element.ROOT).shouldBe(visible);
		return elementRoot;
	}

	public boolean validate() {
		return getRootElement().is(visible);
	}

	public ConnectionsListComponent connectionListComponent() {
		return new ConnectionsListComponent();
	}
}
