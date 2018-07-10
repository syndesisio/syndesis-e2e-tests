# @sustainer: mcada@redhat.com

@webhook
Feature: Webhook extension

  Background: Clean application state
    Given clean application state
    And reset content of "contact" table
    And log into the Syndesis
    And navigate to the "Home" page

#
#  1. Test if webhook GET triggers database operation
#
  @webhook-get
  Scenario: Check message
    When navigate to the "Integrations" page
    And click on the "Create Integration" button to create a new integration.
    And check visibility of visual integration editor
    Then check that position of connection to fill is "Start"

    # select salesforce connection as 'from' point
    When select the "Webhook" connection
    And select "Incoming Webhook" integration action
    And fill in values
      | Webhook Token | test-webhook |
    And click on the "Next" button
    And fill in values
      | Select Type | JSON Instance |
    #only available after type is selected
    And fill in values by element ID
      | specification | {"author":"New Author","title":"Book Title"} |
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into CONTACT values ('Prokop' , 'Dvere', :#COMPANY , 'some lead', '1999-01-01')" value
    And click on the "Done" button
    # add data mapper step
    And click on the "Add a Step" button
    And select "Data Mapper" integration step
    And check visibility of data mapper ui
    And create mapping from "author" to "COMPANY"
    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "webhook-test"
    And click on the "Publish" button
    Then check visibility of "webhook-test" integration details

    When navigate to the "Integrations" page
    Then wait until integration "webhook-test" gets into "Running" state

    When invoke post request to integration "webhook-test" with webhook "test-webhook" and body {"author":"New Author","title":"Book Title"}
    # give integration time to invoke DB request
    And sleep for jenkins delay or "3" seconds
    Then check that query "select * from contact where first_name = 'Prokop' AND last_name = 'Dvere' AND company = 'New Author'" has some output
