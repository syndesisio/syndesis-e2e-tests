package io.syndesis.qe.fragments.common.list;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.have;
import static com.codeborne.selenide.Condition.visible;

import com.codeborne.selenide.CollectionCondition;
import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import io.syndesis.qe.fragments.common.list.actions.ListAction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RowList extends AbstractUiElementsList {

    private static final class Element {
        public static final By ROW = By.xpath("//*[contains(@class,'list-pf-item')]");
        public static final By TITLE = By.xpath("//*[contains(@class,'list-pf-title')]");
    }

    public RowList(By rootElement) {
        super(rootElement);
    }

    @Override
    public ElementsCollection getItemsCollection() {
        ElementsCollection allItems = getRootElement().findAll(Element.ROW);
        return allItems;
    }

    @Override
    public SelenideElement getItem(String title) {
        return getTitle(title).$(By.xpath("./ancestor::*[@class='list-pf-item']")).should(exist);
    }

    @Override
    public SelenideElement getTitle(String title) {
        return getRootElement().shouldBe(visible).$$(Element.TITLE).shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
                .find(have(attribute("title", title)));
    }

    public String getTitleOfItem(SelenideElement item) {
        return item.find(Element.TITLE).shouldBe(visible).getText().trim();
    }
}
