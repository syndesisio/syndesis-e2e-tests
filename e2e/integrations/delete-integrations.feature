@integrations-delete
Feature: Test to verify delete action
  https://issues.jboss.org/browse/IPAAS-290

  Scenario: Create integration as draft and delete it
    When "Camilla" logs into the Syndesis
    And "Camilla" navigates to the "Home" page
    And clicks on the "Create Integration" button to create a new integration.
    Then she is presented with a visual integration editor
    And she is prompted to select a "Start" connection from a list of available connections

    When Camilla selects the "Twitter Example" connection
    And she selects "Mention" integration action
    Then she is prompted to select a "Finish" connection from a list of available connections

    When Camilla selects the "Salesforce Example" connection
    And she selects "Create Opportunity" integration action
    And Camilla clicks on the "Done" button
    Then she is presented with the "Add a Step" button
    And click on the integration save button

    And she defines integration name "Awesome integration for delete"
    And click on the "Save as Draft" button
    Then Camilla is presented with "Awesome integration for delete" integration details
    And Camilla clicks on the "Done" button
    And Integration "Awesome integration for delete" is present in integrations list

    When Camilla deletes the "Awesome integration for delete" integration
    Then she can see success notification
    Then Camilla can not see "Awesome integration for delete" integration anymore