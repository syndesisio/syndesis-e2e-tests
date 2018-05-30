@integrations-database-to-database-with-extension
Feature: Integration - DB to DB with extension

  Background:
    Given clean application state
    Given log into the Syndesis
    Given reset content of "todo" table
    Given reset content of "contact" table
    Given import extensions from syndesis-extensions folder
      | syndesis-extension-log-body |

  @tech-extension-create-integration-with-new-tech-extension
  Scenario: Create
    Then inserts into "contact" table
      | Josef | Stieranka | Istrochem | db |
    And navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    # select postgresDB connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    Then check visibility of page "Periodic SQL Invocation"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "10" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke Stored Procedure" integration action
    And select "add_lead" from "procedureName" dropdown
    And click on the "Done" button

    # add data mapper step
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create mapping from "company" to "company"
    And create mapping from "last_name" to "first_and_last_name"
    And create mapping from "lead_source" to "lead_source"
    And click on the "Done" button

    # add tech extension step
    When click on the "Add a Step" button
    Then check visibility of the "Add a step" link
    And click on the "Add a step" link
    Then select "Log Body" integration step
    And click on the "Done" button

    # finish and save integration
    When click on the "Save as Draft" button
    And set integration name "CRUD4-read-create-inbuilt E2E"
    And click on the "Publish" button

    # assert integration is present in list
    Then check visibility of "CRUD4-read-create-inbuilt E2E" integration details
    And navigate to the "Integrations" page
    And Integration "CRUD4-read-create-inbuilt E2E" is present in integrations list
    # wait for integration to get in active state
    Then wait until integration "CRUD4-read-create-inbuilt E2E" gets into "Published" state

    Then validate add_lead procedure with last_name: "Stieranka", company: "Istrochem", period in ms: "10000"

    When navigate to the "Customizations" page
    And click on the "Extensions" link
    Then check visibility of page "Extensions"

    When select "Delete" action on "Log Message Body" technical extension
    Then check visibility of dialog page "Warning!"
    And check visibility of notification about integrations "CRUD4-read-create-inbuilt E2E" in which is tech extension used

    Then click on the modal dialog "Cancel" button