package io.syndesis.qe.pages.customizations.connectors.wizard.steps;

import com.codeborne.selenide.SelenideElement;
import io.syndesis.qe.fragments.common.form.ApiSpecificationForm;
import io.syndesis.qe.logic.common.wizard.WizardPhase;
import io.syndesis.qe.pages.SyndesisPageObject;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;

import java.io.File;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Slf4j
public class UploadSwaggerSpecification extends SyndesisPageObject implements WizardPhase {

    private ApiSpecificationForm apiSpecificationForm = new ApiSpecificationForm();

    private static class Button {
        public static By NEXT = By.xpath("//button[contains(.,'Next')]");
    }

    private static class Element {
        public static By ROOT = By.cssSelector("syndesis-api-connector-swagger-upload");
    }

    @Override
    public void goToNextWizardPhase() {
        $(Button.NEXT).shouldBe(visible).click();
    }

    @Override
    public SelenideElement getRootElement() {
        return $(Element.ROOT).should(exist);
    }

    @Override
    public boolean validate() {
        return getRootElement().exists();
    }

    public void upload(String source, String url) {
        apiSpecificationForm.upload(source, url);
    }

}
