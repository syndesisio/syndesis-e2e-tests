import { SyndesisComponent } from '../../common/common';
import { by, element, ElementFinder } from 'protractor';
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
    const e = await this.rootElement().element(by.css(`div.row.steps.${type}`));
    return new FlowConnection(type, e);
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

  filterCondition: string;

  predicate: number;

  ruleArray: BasicFilterRule[];

  constructor(filterCondition: string) {
    super();
    this.filterCondition = filterCondition;

    log.info(`filterConditions ${filterCondition}`);

    const filterConditionsArray = filterCondition.split(', ');

    this.predicate = BasicFilterPredicates[filterConditionsArray[0]];

    log.info(`predicate ${filterConditionsArray[0]}`);
    log.info(`predicate number ${this.predicate}`);

    this.ruleArray = [];

    for (let i = 1; i < (filterConditionsArray.length - 2); i = i + 3) {

      log.info(`filter condition ${filterConditionsArray[i]}`);
      log.info(`filter condition ${filterConditionsArray[i + 1]}`);
      log.info(`filter condition ${filterConditionsArray[i + 2]}`);

      let op = filterConditionsArray[i + 1];
      //op = op.split(' ').join('_');
      //op = op.toLocaleLowerCase();

      log.info(`filter condition ${op}`);

      const basicFilterRule = new BasicFilterRule(filterConditionsArray[i], BasicFilterOps[op], filterConditionsArray[i + 2]);
      this.ruleArray.push(basicFilterRule);

      log.info(`filter conditions ` + basicFilterRule.toString());
    }
  }

  fillConfiguration(): P<any> {
    log.info(`fillConfiguration`);

    for (const rule of this.ruleArray) {
      this.setPath(rule.getPath());
      this.setOp(rule.getOp());
      this.setValue(rule.getValue());
        
      /**TODO add rule**/
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

  initialize(): P<any> {
    let parameter;  
      
    return this.getPredicateSelectValue().then((text) => {
      parameter = text;
      return this.getPathInputValue();
    }).then((text) => {
      parameter = parameter + ", " + text;
      return this.getOpSelectValue()
    }).then((text) => {
      parameter = parameter + ", " + text;
      return this.getValueInputValue()
    }).then(((text) => {
      parameter = parameter + ", " + text;
      return this.setParameter(parameter);
    }).bind(this));  
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
  
  getPathInputValue(): P<any> {
    return this.getPathInput().getAttribute('value');
  }

  getValueInput(): ElementFinder {
    log.debug(`Searching basic filter value input`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.valueSelector);
  }

  getValueInputValue(): P<any> {
    return this.getValueInput().getAttribute('value');
  }  

  getOpSelect(): ElementFinder {
    log.debug(`Searching basic filter op select`);
    return this.rootElement().$(IntegrationConfigureBasicFilterStepPage.opSelector);
  }

  async getOpSelectValue(): P<any> {
    log.debug(`Searching basic filter op select checked option`);
    const opValue = await this.getOpSelect().$('option:checked').getText();
    return opValue.trim();
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
    "ALL of the following",
    "ANY of the following",
}

enum BasicFilterOps {
    "Contains",
    "Does Not Contain",
    "Matches Regex",
    "Does Not Match Regex",
    "Starts With",
    "Ends With",
}
