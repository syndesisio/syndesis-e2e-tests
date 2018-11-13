# @sustainer: tplevko@redhat.com

Feature: Integration - Salesforce to DB

  Background: Clean application state
    Given clean SF, removes all leads with email: "jdoesfdb@acme.com"
    And remove all records from table "todo"
    And create SF connection

  @integrations-sf-db @integrations-sf-db-create
  Scenario: On create
    And create SF "salesforce-on-create" action step on field: "Lead"
    And start mapper definition with name: "mapping 1"
    Then MAP using Step 1 and field "/Company" to "/company"
    Then MAP using Step 1 and field "/Email" to "/email"
    Then COMBINE using Step 1 and strategy "Space" into "/first_and_last_name" and sources
      | /FirstName | /LastName |
    And create finish DB invoke stored procedure "add_lead" action step
    When create integration with name: "SF create to DB rest test"
    Then wait for integration with name: "SF create to DB rest test" to become active
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoesfdb@acme.com" and company: "ACME"
    Then validate DB created new lead with first name: "John", last name: "Doe", email: "jdoesfdb@acme.com"

  @integrations-sf-db @integrations-sf-db-delete
  Scenario: On delete
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoesfdb@acme.com" and company: "ACME"
    And create SF "salesforce-on-delete" action step on field: "Lead"
    And start mapper definition with name: "mapping 1"
    Then MAP using Step 1 and field "id" to "/todo"
    And create finish DB invoke sql action step with query "INSERT INTO TODO(task) VALUES(:#todo)"
    When create integration with name: "SF delete to DB rest test"
    Then wait for integration with name: "SF delete to DB rest test" to become active
    Then delete lead from SF with email: "jdoesfdb@acme.com"
    Then validate SF on delete to DB created new task with lead ID as task name

  @integrations-sf-db @integrations-sf-db-update
  Scenario: On update
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoesfdb@acme.com" and company: "ACME"
    And create SF "salesforce-on-update" action step on field: "Lead"
    And start mapper definition with name: "mapping 1"
    Then COMBINE using Step 1 and strategy "Space" into "/todo" and sources
      | /FirstName | /LastName | /Email |
    And create finish DB invoke sql action step with query "INSERT INTO TODO(task) VALUES(:#todo)"
    When create integration with name: "SF update to DB rest test"
    Then wait for integration with name: "SF update to DB rest test" to become active
    Then update SF lead with email "jdoesfdb@acme.com" to first name: "Joe", last name "Carrot", email "jdoesfdb@acme.com", company name "EMCA"
    Then validate DB created new lead with first name: "Joe", last name: "Carrot", email: "jdoesfdb@acme.com"
