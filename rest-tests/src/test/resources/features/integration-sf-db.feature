Feature: sf scenarios

  Background: Clean application state
    Given clean SF, removes all leads with email: "jdoe@acme.com"
	And remove all records from DB
    And create SF connection

  @integrations-sf-db
  Scenario: SF action on create - DB integration
    And create SF "create" action step on field: "Lead"
    And create mapper step using template: "sf-create-db"
    And create DB step from template add_lead
    When create integration with name: "SF create to DB rest test"
    Then wait for integration with name: "SF create to DB rest test" to become active
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoe@acme.com" and company: "ACME"
    Then validate DB created new lead with first name: "John", last name: "Doe", email: "jdoe@acme.com"

  @integrations-sf-db
  Scenario: SF action on delete - DB integration
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoe@acme.com" and company: "ACME"
    And create SF "delete" action step on field: "Lead"
    And create mapper step using template: "sf-delete-db"
    And create DB insert taks step
    When create integration with name: "SF delete to DB rest test"
    Then wait for integration with name: "SF delete to DB rest test" to become active
    Then delete lead with first name "John" and last name "Doe"
    Then validate SF on delete to DB created new task with lead ID as task name

  @integrations-sf-db
  Scenario: SF action on update - DB integration
    Then create SF lead with first name: "John", last name: "Doe", email: "jdoe@acme.com" and company: "ACME"
    And create SF "update" action step on field: "Lead"
    And create mapper step using template: "sf-update-db"
    And create DB insert taks step
    When create integration with name: "SF update to DB rest test"
    Then wait for integration with name: "SF update to DB rest test" to become active
    Then update SF lead with email "jdoe@acme.com" to first name: "Joe", last name "Carrot", email "jcarrot@acme.com", company name "EMCA"
    Then validate SF to DB created new lead with first name: "Joe", last name: "Carrot", email: "jcarrot@acme.com"
