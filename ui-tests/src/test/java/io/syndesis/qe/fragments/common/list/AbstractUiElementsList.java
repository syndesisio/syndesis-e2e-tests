package io.syndesis.qe.fragments.common.list;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import io.syndesis.qe.fragments.common.list.actions.ListAction;
import io.syndesis.qe.pages.SyndesisPageObject;

public abstract class AbstractUiElementsList extends SyndesisPageObject {

    private By rootElement;

    public AbstractUiElementsList(By rootElement) {
        this.rootElement = rootElement;
    }

    @Override
    public SelenideElement getRootElement() {
        return $(rootElement).should(exist);
    }

    @Override
    public boolean validate() {
        return $(rootElement).is(visible);
    }

    abstract ElementsCollection getItemsCollection();

    abstract SelenideElement getItem(String title);

    abstract SelenideElement getTitle(String title);

    abstract boolean invokeActionOnItem(String title, ListAction action);
}
