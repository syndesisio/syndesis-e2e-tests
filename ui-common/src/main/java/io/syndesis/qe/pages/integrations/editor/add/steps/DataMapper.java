package io.syndesis.qe.pages.integrations.editor.add.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import io.syndesis.qe.pages.ModalDialogPage;
import io.syndesis.qe.pages.SyndesisPageObject;
import io.syndesis.qe.utils.Alert;
import io.syndesis.qe.utils.ByUtils;
import io.syndesis.qe.utils.Conditions;
import io.syndesis.qe.utils.TestUtils;

import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataMapper extends SyndesisPageObject {

    private static final class Element {
        public static final By ROOT = ByUtils.dataTestId("datamapper-root-view");
        public static final By COLLECTION_ROOT = ByUtils.containsId("field-atlas:");

        public static final By SOURCE_COLUMN = ByUtils.dataTestId("column-source-area");
        public static final By TARGET_COLUMN = ByUtils.dataTestId("column-target-area");

        public static final By DATA_BUCKET_ROOT = By.className("pf-c-card");
        public static final String DATA_BUCKET_BUTTON = "expand-collapse-%s";
        public static final By DATA_BUCKET_BODY = By.className("pf-c-card__body"); //css-8jvg5p

        public static final By EXPANDABLE_ROW = By.cssSelector("div[role=\"treeitem\"][aria-expanded]"); //collection row
        public static final By MAPPING_FIELD_ITEM_ROW = By.cssSelector("div[role=\"treeitem\"]:not([aria-expanded])");

        public static final By CONNECT_TO_MAPPING_BUTTON = ByUtils.dataTestId("connect-to-the-selected-mapping-button");
        public static final By CREATE_NEW_MAPPING_BUTTON = ByUtils.dataTestId("create-new-mapping-button");
        public static final By ADD_NEW_MAPPING_BUTTON = ByUtils.dataTestId("add-new-mapping-button");

        public static final By CLOSING_MAPPING_DETAIL_BUTTON = ByUtils.dataTestId("close-mapping-detail-button");

        public static final By CREATE_PROPERTY_BUTTON = ByUtils.dataTestId("create-property-button");
        public static final String PROPERTY_NAME = "property-name-text-input";
        public static final String PROPERTY_VALUE = "property-value-text-input";
        public static final String PROPERTY_TYPE = "property-type-form-select";

        public static final By CREATE_CONSTANT_BUTTON = ByUtils.dataTestId("create-constant-button");
        public static final String CONSTANT_NAME = "constant-name-text-input";
        public static final String CONSTANT_TYPE = "constant-type-form-select";
    }

    @Override
    public SelenideElement getRootElement() {
        return $(Element.ROOT).shouldBe(visible);
    }

    @Override
    public boolean validate() {
        return getRootElement().is(visible);
    }

    /**
     * This method expands the only collections as root elements. The other collections (as child element somewhere on the nested level)
     * are expanded only when they are used ( in getMappingItemRow / getCollectionElement methods).
     * Expanding all collections here would be ineffective because some connector has lots of collection on multiple nested levels (e.g. fhir)
     */
    public void openDataMapperCollectionElement() {
        // Give page and elements time to load in slower environment
        // before pressing mapper collection items
        boolean mapperCollectionFound = TestUtils.waitForNoFail(() -> $(Element.COLLECTION_ROOT).is(visible),
            1, 3);

        //during investigating instability on chrome, i noticed that the datamapper is reloaded after 2-3sec, so after that
        // the all collection are not expanded. I am not able to reproduce it manually, maybe some problem with chrome driver
        TestUtils.sleepIgnoreInterrupt(4000);
        if (mapperCollectionFound) {
            ElementsCollection dataMapperCollections = getSourceElementColumn().$$(Element.COLLECTION_ROOT).exclude(Conditions.STALE_ELEMENT);
            log.info("DataMapper Source column contains {} collections for expand", dataMapperCollections.size());
            expandCollection(dataMapperCollections);

            dataMapperCollections = getTargetElementColumn().$$(Element.COLLECTION_ROOT).exclude(Conditions.STALE_ELEMENT);
            log.info("DataMapper Target column contains {} collections for expand", dataMapperCollections.size());
            expandCollection(dataMapperCollections);
        }
    }

    private void expandCollection(ElementsCollection collectionsCollection) {
        for (SelenideElement element : collectionsCollection) {
            if (element.attr("aria-expanded").equals("false")) {
                // collection has not been expanded yet
                element.click();
            }
        }
    }

    public void addConstant(String value, String type) {
        getSourceElementColumn().find(Element.CREATE_CONSTANT_BUTTON).shouldBe(visible).click();
        ModalDialogPage modalDialogPage = new ModalDialogPage();
        assertThat(modalDialogPage.getTitleText()).contains("Create Constant");
        modalDialogPage.fillInputByDataTestid(Element.CONSTANT_NAME, value);
        modalDialogPage.selectValueByDataTestid(Element.CONSTANT_TYPE, type);
        modalDialogPage.getButton("Confirm").shouldBe(visible).click();
        this.removeAllAlertsFromPage(Alert.WARNING);
        //when the modalDialog is closed, the tooltip remains open, maybe it causes instability on chrome
        getSourceElementColumn().find(By.className("pf-c-title")).click();
    }

    public void addProperty(String name, String value, String type) {
        getSourceElementColumn().find(Element.CREATE_PROPERTY_BUTTON).shouldBe(visible).click();
        ModalDialogPage modalDialogPage = new ModalDialogPage();
        assertThat(modalDialogPage.getTitleText()).contains("Create Property");
        modalDialogPage.fillInputByDataTestid(Element.PROPERTY_NAME, name);
        modalDialogPage.fillInputByDataTestid(Element.PROPERTY_VALUE, value);
        modalDialogPage.selectValueByDataTestid(Element.PROPERTY_TYPE, type);
        modalDialogPage.getButton("Confirm").shouldBe(visible).click();
        this.removeAllAlertsFromPage(Alert.WARNING);
        //when the modalDialog is closed, the tooltip remains open, maybe it causes instability on chrome
        getSourceElementColumn().find(By.className("pf-c-title")).click();
    }

    public void doCreateMapping(String source, String target) {
        createNewMapping(null, source, null, target);
        getRootElement().find(Element.CLOSING_MAPPING_DETAIL_BUTTON).shouldBe(visible).click();
    }

    public void doCreateMappingWithDataBucket(String sourceBucket, String source, String targetBucket, String target) {
        createNewMapping(sourceBucket, source, targetBucket, target);
        getRootElement().find(Element.CLOSING_MAPPING_DETAIL_BUTTON).shouldBe(visible).click();
    }

    /**
     * Parse multiple properties e.g. first_name ; last_name etc.
     */
    private void createNewMapping(String sourceBucket, String source, String targetBucket, String target) {
        log.info("Mapping {} -> {}", source, target);

        // create a brand new mapping
        getRootElement().find(Element.ADD_NEW_MAPPING_BUTTON).shouldBe(visible).click();

        SelenideElement src = this.getSourceElementColumn();
        SelenideElement dest = this.getTargetElementColumn();

        if (sourceBucket != null && !sourceBucket.isEmpty()) {
            src = this.getDataBucketElement(sourceBucket);
        }
        if (targetBucket != null && !targetBucket.isEmpty()) {
            dest = this.getDataBucketElement(targetBucket);
        }

        if (source.contains(";")) {
            String[] array = source.split(";");
            log.info("Switching to the Data Mapper SOURCE column");
            for (String str : array) {
                this.selectMapping(str.trim(), src);
            }
            log.info("Switching to the Data Mapper TARGET column");
            this.selectMapping(target, dest);
        } else if (target.contains(";")) {
            log.info("Switching to the Data Mapper SOURCE column");
            this.selectMapping(source, src);
            log.info("Switching to the Data Mapper TARGET column");
            String[] array = target.split(";");
            for (String str : array) {
                this.selectMapping(str.trim(), dest);
            }
        } else {
            log.info("Switching to the Data Mapper SOURCE column");
            this.selectMapping(source, src);
            log.info("Switching to the Data Mapper TARGET column");
            this.selectMapping(target, dest);
        }
    }

    /**
     * The dot in mappingName can indicate it's a nested element. But first we perform a check if the element does not exist.
     *
     * @param mappingName for instance "User.ScreenName"
     * @param containerElement start searching mapping fields from here
     */
    public void selectMapping(String mappingName, SelenideElement containerElement) {
        log.info("Selecting '{}' mapping field", mappingName);
        int nestedLevel = 1; // root level
        SelenideElement mappingItemRow = getMappingItemRow(mappingName, containerElement, nestedLevel);
        if (mappingItemRow == null) {
            // Mapping field doesn't exist, it can be a part of collection on any nested level.
            // First, it needs to select only parent container (collection) and find out nested level of the mapping field
            List<String> separatedElement = Arrays.asList(mappingName.trim().split("\\."));
            SelenideElement parentContainer = containerElement;

            if (separatedElement.size() == 1) {
                log.info(
                    "Mapping field is nested but input param doesn't contain parent information. Probably the field is part of unnamed collection");
                nestedLevel++;
            } else {
                log.info("Mapping field is nested. Parsing parents and select the final collection and nested level");
                // iterate through parents to get close to the final level
                // -1 because the last element is not a parent but the desired mapping field
                for (int i = 0; i < separatedElement.size() - 1; i++) {
                    log.info("Searching for '{}' collection on the {} nested level", separatedElement.get(i), nestedLevel);
                    parentContainer = getCollectionElement(separatedElement.get(i), parentContainer, nestedLevel);
                    nestedLevel++;
                    log.info("In the {}. parent on the {} nested level. Parent (Collection) name is '{}'", i + 1, nestedLevel,
                             separatedElement.get(i));
                }
            }
            mappingItemRow = getMappingItemRow(separatedElement.get(separatedElement.size() - 1), parentContainer, nestedLevel);
        }
        if (mappingItemRow == null) {
            fail("The mapping field '" + mappingName + "' cannot be found! Check log and screenshot whether DataMapper contains that field." +
                     " If it is a part of nested collection, you have to add all parent names. See mapping step's JavaDoc");
        }
        mappingItemRow.scrollIntoView(true);
        TestUtils.sleepIgnoreInterrupt(500);

        //if click cause undesired behavior in the future, hover() can be used instead
        mappingItemRow.click();

        if (mappingItemRow.find(Element.CONNECT_TO_MAPPING_BUTTON).exists()) {
            if (mappingItemRow.find(Element.CONNECT_TO_MAPPING_BUTTON).is(disabled)) {
                log.info("No active mapping found, creating a new one");
                mappingItemRow.find(Element.CREATE_NEW_MAPPING_BUTTON).shouldBe(visible).click();
                this.removeAllAlertsFromPage(Alert.WARNING);
            } else {
                log.info("Active mapping found, connecting to that");
                mappingItemRow.find(Element.CONNECT_TO_MAPPING_BUTTON).click();
            }
        }
    }

    /**
     * Return row for the particular mapping field under the specific element (e.g. root element or some collection)
     *
     * @param name - field name
     * @param containerElement - element for searching
     * @param nestedLevel - on which nested level the mapping field is located (e.g. 'name' field can be in the root level (level 1)
     * and also in the collection body.name (level2))
     */
    private SelenideElement getMappingItemRow(String name, SelenideElement containerElement, int nestedLevel) {
        // if parent element has not been expanded yet
        if ("false".equals(containerElement.getAttribute("aria-expanded"))) {
            containerElement.click();
        }
        ElementsCollection rowsWithName = containerElement
            .findAll(Element.MAPPING_FIELD_ITEM_ROW)
            .filter(Condition.attribute("aria-level", String.valueOf(nestedLevel)))
            // if the row has same name or the name is a part of sentence (must be space before and after, for properties in datamapper `properties
            // = value`)
            .filter(Condition.matchText("^(.* )?" + name + "( .*)?$"));
        if (rowsWithName.size() == 0) {
            return null;
        } else if (rowsWithName.size() > 1) {
            fail("Too many rows with the same name. Name: " + name + " Nested Level: " + nestedLevel +
                     ". Probably more data buckets have the field element with the same name. In that case, use different step when explicitly " +
                     "select " +
                     "data bucket");
        }
        return rowsWithName.get(0);
    }

    /**
     * Return element for the whole collection
     *
     * @param name - collection name
     * @param containerElement - element for searching
     * @param nestedLevel - on which nested level the collection is located (e.g. 'person' collection field can be in the root level (level 1)
     * and also in another collection response.person (level2))
     */
    private SelenideElement getCollectionElement(String name, SelenideElement containerElement, int nestedLevel) {
        // if parent element has not been expanded yet
        if ("false".equals(containerElement.getAttribute("aria-expanded"))) {
            containerElement.click();
        }
        // find all expandable row on particular nested level
        ElementsCollection allExpandableRows = containerElement
            .findAll(Element.EXPANDABLE_ROW)
            .filter(Condition.attribute("aria-level", String.valueOf(nestedLevel)));
        // get only row which contains particular collection (defined by collection name in the datatestid)
        List<SelenideElement> rowsWithName =
            allExpandableRows.stream().filter(row -> row.find(ByUtils.containsDataTestId("field-group-" + name + "-expanded")).exists())
                .collect(Collectors.toList());
        if (rowsWithName.size() == 0) {
            fail("Collection with the name: " + name + " doesn't exist on the nested Level: " + nestedLevel);
        } else if (rowsWithName.size() > 1) {
            fail("Too many rows with the same name. " +
                     "Probably the element contains more collections with the same name in the different nested level." +
                     "Searched collection name: " + name + " Actual nested level: " + nestedLevel);
        }
        return rowsWithName.get(0);
    }

    /**
     * Return element contains only source mapping items
     */
    private SelenideElement getSourceElementColumn() {
        ElementsCollection sourceColumn = getRootElement().findAll(Element.SOURCE_COLUMN);
        assertThat(sourceColumn.size()).isEqualTo(1);
        return sourceColumn.first();
    }

    /**
     * Return element contains only target mapping items
     */
    private SelenideElement getTargetElementColumn() {
        ElementsCollection targetColumn = getRootElement().findAll(Element.TARGET_COLUMN);
        assertThat(targetColumn.size()).isEqualTo(1);
        return targetColumn.first();
    }

    /**
     * Get all mapping element by name (source and target)
     */
    public ElementsCollection getAllItemsWithName(String name) {
        return getRootElement()
            .findAll(Element.MAPPING_FIELD_ITEM_ROW)
            .filter(Condition.matchText(
                "^(.*\\s)?" + name + "(\\s.*)?$"));
    }

    public SelenideElement getDataBucketElement(String bucketName) {
        return getRootElement().findAll(Element.DATA_BUCKET_ROOT)
            .filter(Condition.matchesText(bucketName))
            .shouldHaveSize(1)
            .get(0)
            .shouldBe(visible);
    }

    public void openBucket(String bucketName) {
        SelenideElement dataBucketElement = getDataBucketElement(bucketName);
        if (!isDataBucketOpen(bucketName)) {
            dataBucketElement.find(ByUtils.containsDataTestId(String.format(Element.DATA_BUCKET_BUTTON, bucketName))).shouldBe(visible).click();
        }
    }

    public void closeBucket(String bucketName) {
        SelenideElement dataBucketElement = getDataBucketElement(bucketName);
        if (isDataBucketOpen(bucketName)) {
            dataBucketElement.find(ByUtils.containsDataTestId(String.format(Element.DATA_BUCKET_BUTTON, bucketName))).shouldBe(visible).click();
        }
    }

    public boolean isDataBucketOpen(String bucketName) {
        SelenideElement dataBucketElement = getDataBucketElement(bucketName);
        return dataBucketElement.find(Element.DATA_BUCKET_BODY).isDisplayed();
    }
}
