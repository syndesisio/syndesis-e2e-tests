import { Utils } from '../../common/utils';
import { SyndesisComponent } from '../../common/common';
import { by, element, browser, ElementFinder, ElementArrayFinder, ExpectedConditions } from 'protractor';
import { P } from '../../common/world';
import { ConnectionsListComponent } from '../../connections/list/list.po';
import { log } from '../../../src/app/logging';


export class FlowConnection {

  constructor(public type: string, public element: ElementFinder) {
  }

  /**
   * Check if this element is active
   * @returns {webdriver.promise.Promise<boolean>}
   */
  isActive(): P<boolean> {
    return this.element.element(by.css('p.icon.active')).isPresent();
  }

}

export class FlowViewComponent implements SyndesisComponent {
  static readonly nameSelector = 'input.form-control.integration-name';
  static readonly stepSelector = 'parent-step';
  static readonly activeStepSelector = 'div[class="parent-step active"]';

  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-flow-view'));
  }

  getIntegrationName(): P<string> {
    return this.rootElement()
      .element(by.css(FlowViewComponent.nameSelector))
      .getAttribute('value');
  }

  /**
   * Get div
   * @param type (start|finish)
   */
  async flowConnection(type: string): P<FlowConnection> {
    type = type.toLowerCase();
    const e = await this.rootElement().element(by.css(`div.step.${type}`));
    return new FlowConnection(type, e);
  }

  async getStepsArray(): P<any> {
    const stepFactory = new StepFactory();
    const steps = this.rootElement().all(by.className(FlowViewComponent.stepSelector));

    const count = await steps.count();
    const stepsArray = new Array();

    for (let i = 1; i < (count - 1); i++) {
      steps.get(i).click();

      const title = this.rootElement().element(by.css(FlowViewComponent.activeStepSelector));

      const text = await title.getText();
      const stepPage = stepFactory.getStep(text, '');

      try {
        await browser.wait(ExpectedConditions.visibilityOf(stepPage.rootElement()), 6000, 'No root element');
      } catch (e) {
        return P.reject(e);
      }

      await stepPage.initialize();

      stepsArray.push(stepPage.getParameter());
    }

    const allButtonsByTitle = element.all(by.buttonText('Done'));
    const doneButton = await allButtonsByTitle.filter(function(elem) {
      return elem.isDisplayed().then(function(displayedElement){
        return displayedElement;
      });
    }).first();

    doneButton.click();

    return stepsArray;
  }
}


export class ListActionsComponent implements SyndesisComponent {
  rootElement(): ElementFinder {
    return element(by.css('syndesis-list-actions'));
  }

  selectAction(name: string): P<any> {
    log.info(`searching for integration action '${name}'`);
    return this.rootElement().$(`div.action[title="${name}"]`).click();
  }

}

export class ConnectionSelectComponent implements SyndesisComponent {
  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-connection-select'));
  }

  connectionListComponent(): ConnectionsListComponent {
    return new ConnectionsListComponent();
  }


}


export class IntegrationBasicsComponent implements SyndesisComponent {
  static readonly nameSelector = 'input[name="nameInput"]';
  static readonly descriptionSelector = 'textarea[name="descriptionInput"]';

  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-integration-basics'));
  }

  setName(name: string): P<any> {
    log.debug(`setting integration name to ${name}`);
    return this.rootElement().$(IntegrationBasicsComponent.nameSelector).sendKeys(name);
  }

  setDescription(description: string): P<any> {
    return this.rootElement().$(IntegrationBasicsComponent.descriptionSelector).sendKeys(description);
  }


}

export class IntegrationEditPage implements SyndesisComponent {

  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-edit-page'));
  }

  actionConfigureComponent(): ActionConfigureComponent {
    return new ActionConfigureComponent();
  }


  flowViewComponent(): FlowViewComponent {
    return new FlowViewComponent();
  }

  connectionSelectComponent(): ConnectionSelectComponent {
    return new ConnectionSelectComponent();
  }

  basicsComponent(): IntegrationBasicsComponent {
    return new IntegrationBasicsComponent();
  }


}

export class IntegrationAddStepPage implements SyndesisComponent {
  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-step-select'));
  }

  addStep(stepName: string): P<any> {
    log.info(`searching for step ${stepName}`);
    return this.rootElement().$(`div.step[title="${stepName}"]`).click();
  }
}

export class StepFactory {

  getStep(stepType: string, parameter: string): IntegrationConfigureStepPage {
    if (stepType == null) {
      return null;
    }
    if (stepType.toUpperCase() === 'LOG') {
      return new IntegrationConfigureLogStepPage(parameter);
    } else if (stepType.toUpperCase() === 'BASIC FILTER') {
      return new IntegrationConfigureBasicFilterStepPage(parameter);
    }

    return null;
  }
}

export abstract class IntegrationConfigureStepPage implements SyndesisComponent {
  rootElement(): ElementFinder {
    log.debug(`getting root element for step configuration page`);
    return element(by.css('syndesis-integrations-step-configure'));
  }

  abstract fillConfiguration(): P<any>;

  abstract validate(): P<any>;

  abstract initialize(): P<any>;

  abstract setParameter(parameter: string): void;

  abstract getParameter(): string;
}

export class IntegrationConfigureLogStepPage extends IntegrationConfigureStepPage {
  static readonly messageSelector = 'input[name="message"]';

  logMessage: string;

  constructor(logMessage: string) {
    super();
    this.logMessage = logMessage;
  }

  fillConfiguration(): P<any> {
    return this.setMessage(this.logMessage);
  }

  validate(): P<any> {
    log.debug(`validating configuration page`);
    return this.getMessageInput().isPresent();
  }

  initialize(): P<any> {
    return this.getMessageInputValue().then((function(text) {
      this.setParameter(text);
    }).bind(this));
  }

  setMessage(message: string): P<any> {
    log.info(`setting integration step message to ${message}`);
    return this.rootElement().$(IntegrationConfigureLogStepPage.messageSelector).sendKeys(message);
  }

  setParameter(logMessage: string): void {
    this.logMessage = logMessage;
  }

  getMessageInput(): ElementFinder {
    log.debug(`searching for message input`);
    return this.rootElement().$(IntegrationConfigureLogStepPage.messageSelector);
  }

  getMessageInputValue(): P<any> {
    return this.getMessageInput().getAttribute('value');
  }

  getParameter(): string {
    return this.logMessage;
  }
}

export class IntegrationConfigureBasicFilterStepPage extends IntegrationConfigureStepPage {
  static readonly predicateSelector = 'select[id="predicate"]';
  static readonly predicateOptionSelector = 'option[name="predicate"]';

  static readonly pathSelector = 'input[name="path"]';
  static readonly valueSelector = 'input[name="value"]';
  static readonly opSelector = 'select[name="op"]';
  static readonly opOptionSelector = 'option[name="op"]';

  static readonly addRuleSelector = 'a.add-rule';

  filterCondition: string;

  predicate: number;

  ruleArray: BasicFilterRule[];

  constructor(filterCondition: string) {
    super();
    this.filterCondition = filterCondition;
    const filterConditionsArray = this.filterCondition.split(', ');

    this.predicate = BasicFilterPredicates[filterConditionsArray[0]];

    this.ruleArray = [];

    for (let i = 1; i < (filterConditionsArray.length - 2); i = i + 3) {
      const op = filterConditionsArray[i + 1];

      const basicFilterRule = new BasicFilterRule(filterConditionsArray[i], BasicFilterOps[op], filterConditionsArray[i + 2]);
      this.ruleArray.push(basicFilterRule);
    }
  }

  async fillConfiguration(): P<any> {

    for (let i = 0; i < this.ruleArray.length; i++) {
      await this.setLatestPathInput(this.ruleArray[i].getPath());
      await this.setLatestOpSelect(this.ruleArray[i].getOp());
      await this.setLatestValueInput(this.ruleArray[i].getValue());

      if (i !== (this.ruleArray.length - 1)) {
        const addRuleLink = await this.rootElement().$(IntegrationConfigureBasicFilterStepPage.addRuleSelector);
        await addRuleLink.click();
      }
    }

    return this.setPredicate(this.predicate);
  }

  validate(): P<any> {
    log.debug(`validating configuration page`);

    const predicatPresent = this.getPredicateSelect().isPresent();
    const pathPresent = this.getPathInput().isPresent();
    const valuePresent = this.getValueInput().isPresent();
    const opPresent = this.getOpSelect().isPresent();

    return (predicatPresent && pathPresent && valuePresent && opPresent);
  }

  async initialize(): P<any> {
    const predicateSelectValue = await this.getPredicateSelectValue();

    const pathInputValues = await this.getPathInputAllValues();
    const opSelectValues = await this.getOpSelectAllValues();
    const valueInputValues = await this.getValueInputAllValues();

    let parameter = predicateSelectValue;

    this.ruleArray = [];

    for (let i = 0; i < pathInputValues.length; i++) {
      const basicFilterRule = new BasicFilterRule(pathInputValues[i], opSelectValues[i], valueInputValues[i]);

      this.ruleArray.push(basicFilterRule);

      parameter = parameter + ', ' + pathInputValues[i] + ', ' + opSelectValues[i] + ', ' + valueInputValues[i];
    }

    return this.setParameter(parameter);
  }

  async addRule(ruleString: string): P<any> {
    const ruleStringArray = ruleString.split(', ');
    const op = ruleStringArray[1];
    const basicFilterRule = new BasicFilterRule(ruleStringArray[0], BasicFilterOps[op], ruleStringArray[2]);

    const addRuleLink = await this.rootElement().$(IntegrationConfigureBasicFilterStepPage.addRuleSelector);
    await addRuleLink.click();

    this.setLatestPathInput(basicFilterRule.getPath());
    this.setLatestOpSelect(basicFilterRule.getOp());
    this.setLatestValueInput(basicFilterRule.getValue());

    this.ruleArray.push(basicFilterRule);
  }

  setParameter(filterCondition: string): void {
    this.filterCondition = filterCondition;
  }

  setPredicate(predicate: number): P<any> {
    log.info(`setting basic filter step predicate to option number ${predicate}`);
    const predicateOptions = this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.predicateOptionSelector));
    return predicateOptions.then((options) => {
      options[this.predicate].click();
    });
  }

  setOp(op: number): P<any> {
    log.info(`setting basic filter step predicate to option number ${op}`);
    const opOptions = this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.opOptionSelector));
    return opOptions.then((options) => {
      options[op].click();
    });
  }

  setPath(path: string): P<any> {
    log.info(`setting basic filter step path to ${path}`);
    const pathInput = this.rootElement().$(IntegrationConfigureBasicFilterStepPage.pathSelector);

    return pathInput.clear().then(function() {
      pathInput.sendKeys(path);
    });
  }

  setValue(value: string): P<any> {
    log.info(`setting basic filter step value to ${value}`);
    const valueInput = this.rootElement().$(IntegrationConfigureBasicFilterStepPage.valueSelector);

    return valueInput.clear().then(function() {
      valueInput.sendKeys(value);
    });
  }

  async setLatestOpSelect(op: number): P<any> {
    log.info(`setting basic filter step predicate to option number ${op}`);
    const opSelectArray = await this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.opSelector));
    const opSelect = opSelectArray[opSelectArray.length - 1];
    const opOptions = await opSelect.all(by.css(IntegrationConfigureBasicFilterStepPage.opOptionSelector));

    return opOptions[op].click();
  }

  async setLatestPathInput(path: string): P<any> {
    log.info(`setting basic filter step path to ${path}`);
    const pathInputArray = await this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.pathSelector));
    const pathInput = pathInputArray[pathInputArray.length - 1];

    await pathInput.clear();
    return pathInput.sendKeys(path);
  }

  async setLatestValueInput(value: string): P<any> {
    log.info(`setting basic filter step value to ${value}`);
    const valueInputArray = await this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.valueSelector));
    const valueInput = valueInputArray[valueInputArray.length - 1];

    await valueInput.clear();
    return valueInput.sendKeys(value);
  }

  getParameter(): string {
    return this.filterCondition;
  }

  getPredicateSelect(): ElementFinder {
    log.debug(`Searching basic filter predicate select`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.predicateSelector);
  }

  async getPredicateSelectValue(): P<any> {
    log.debug(`Searching basic filter predicate select checked option`);
    const predicateValue = await this.getPredicateSelect().$('option:checked').getText();
    return predicateValue.trim();
  }

  getPathInput(): ElementFinder {
    log.debug(`Searching basic filter path input`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.pathSelector);
  }

  getPathInputs(): ElementArrayFinder {
    log.debug(`Searching basic filter path input`);
    return this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.pathSelector));
  }

  getPathInputValue(): P<any> {
    return this.getPathInput().getAttribute('value');
  }

  async getPathInputAllValues(): P<any> {
    const pathInputArray = this.getPathInputs();
    const count = await pathInputArray.count();

    const pathInputValues = new Array();

    for (let i = 0; i < count; i++) {
      const value = await pathInputArray.get(i).getAttribute('value');
      pathInputValues.push(value);
    }

    return pathInputValues;
  }

  getValueInput(): ElementFinder {
    log.debug(`Searching basic filter value input`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.valueSelector);
  }

  getValueInputs(): ElementArrayFinder {
    log.debug(`Searching basic filter value input`);
    return this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.valueSelector));
  }

  getValueInputValue(): P<any> {
    return this.getValueInput().getAttribute('value');
  }

  async getValueInputAllValues(): P<any> {
    const valueInputArray = this.getValueInputs();
    const count = await valueInputArray.count();

    const valueInputValues = new Array();

    for (let i = 0; i < count; i++) {
      const value = await valueInputArray.get(i).getAttribute('value');
      valueInputValues.push(value);
    }

    return valueInputValues;
  }

  getOpSelect(): ElementFinder {
    log.debug(`Searching basic filter op select`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.opSelector);
  }

  getOpSelects(): ElementArrayFinder {
    log.debug(`Searching basic filter op selects`);
    return this.rootElement().all(by.css(IntegrationConfigureBasicFilterStepPage.opSelector));
  }

  async getOpSelectValue(): P<any> {
    log.debug(`Searching basic filter op select checked option`);
    const opValue = await this.getOpSelect().$('option:checked').getText();
    return opValue.trim();
  }

  async getOpSelectAllValues(): P<any> {
    log.debug(`Searching basic filter op select checked options`);

    const opSelectArray = this.getOpSelects().all(by.css('option:checked'));
    const count = await opSelectArray.count();

    const opSelectValues = new Array();

    for (let i = 0; i < count; i++) {
      const value = await opSelectArray.get(i).getText();
      opSelectValues.push(value.trim());
    }

    return opSelectValues;
  }
}

export class BasicFilterRule {
  path: string;
  op: number;
  value: string;

  constructor(path: string, op: number, value: string) {
    this.path = path;
    this.op = op;
    this.value = value;
  }

  getPath(): string {
    return this.path;
  }

  getOp(): number {
    return this.op;
  }

  getValue(): string {
    return this.value;
  }

  toString(): string {
    return 'Path: ' + this.path + ' Op: ' + this.op + ' Value: ' + this.value;
  }
}

enum BasicFilterPredicates {
    'ALL of the following',
    'ANY of the following',
}

enum BasicFilterOps {
    'Contains',
    'Does Not Contain',
    'Matches Regex',
    'Does Not Match Regex',
    'Starts With',
    'Ends With',
}

export class ActionConfigureComponent implements SyndesisComponent {
  rootElement(): ElementFinder {
    return element(by.css('syndesis-integrations-action-configure'));
  }

  fillInput(inputId: string, value: string): P<any> {
    const input = this.getInput(inputId);
    return input.sendKeys(value);
  }

  getInput(inputId: string): ElementFinder {
    return this.rootElement().element(by.id(inputId));
  }
}

export class TwitterSearchActionConfigureComponent extends ActionConfigureComponent {
  static readonly idSelector = 'keywords';

  keywordsElement(): ElementFinder {
    return element(by.id(TwitterSearchActionConfigureComponent.idSelector));
  }

  fillKeywordsValue(value: string): P<any> {
    log.debug(`setting keywords element of twitter search with value: ${value}`);
    const fillMap = new Map();
    fillMap.set(TwitterSearchActionConfigureComponent.idSelector, value);
    return Utils.fillForm(fillMap, this.rootElement(), 'id');
  }
}
