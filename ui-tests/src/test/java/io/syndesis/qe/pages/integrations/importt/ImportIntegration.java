package io.syndesis.qe.pages.integrations.importt;

import com.codeborne.selenide.SelenideElement;
import io.syndesis.qe.CustomWebDriverProvider;
import io.syndesis.qe.pages.SyndesisPageObject;
import io.syndesis.qe.utils.DragAndDropFile;
import org.openqa.selenium.By;

import java.io.File;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class ImportIntegration extends SyndesisPageObject {

    private static final class Element {
        public static final By ROOT = By.cssSelector("syndesis-integration-import-export");
        public static final By FILE_INPUT = By.cssSelector("input[type='file']");
        public static final By FINISHED_PROGRESS_BAR = By.className("pficon-ok");
        public static final By DRAG_AND_DROP_PLACE = By.className("syn-drop-zone");
    }

    @Override
    public SelenideElement getRootElement() {
        return $(Element.ROOT);
    }

    public boolean validate() {
        return getRootElement().is(visible);
    }

    /**
     * Import integration from zip file
     *
     * @param file
     */
    public void importIntegration(File file) {
        getRootElement().find(Element.FILE_INPUT).shouldBe(visible).uploadFile(file);
        $(Element.FINISHED_PROGRESS_BAR).shouldBe(visible);
        getButton("Done").shouldBe(visible).click();
    }


    /**
     * Import integration from browsers default download dir
     *
     * @param integrationName name of integration which was exported to browsers default download dir
     * @throws InterruptedException
     */
    public void importIntegration(String integrationName) throws InterruptedException {

        String filePath = CustomWebDriverProvider.DOWNLOAD_DIR + File.separator + integrationName + "-export.zip";
        File exportedIntegrationFile = new File(filePath);

        importIntegration(exportedIntegrationFile);

    }

    /**
     * Import integration from browsers default download dir via drag'n'drop feature
     *
     * @param integrationName
     */
    public void importIntegrationViaDragAndDrop(String integrationName) {

        String filePath = CustomWebDriverProvider.DOWNLOAD_DIR + File.separator + integrationName + "-export.zip";

        DragAndDropFile.dragAndDropFile(new File(filePath),
                $(Element.DRAG_AND_DROP_PLACE).shouldBe(visible),
                Element.FINISHED_PROGRESS_BAR);

        getButton("Done").shouldBe(visible).click();
    }
}
