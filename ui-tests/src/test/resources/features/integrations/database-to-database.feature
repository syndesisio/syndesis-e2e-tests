@integrations-database-to-database
Feature: Integration - DB to DB

  Background: Clean application state
    Given clean application state
    Given log into the Syndesis

    Given reset content of "todo" table
    Given reset content of "contact" table
#
#  1. select - update
#
  @db-connection-crud-1-read-update
  Scenario: Read & update operations

    Then inserts into "todo" table
      | Joe |

    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    Then check "Done" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "UPDATE TODO SET completed=1 WHERE TASK = :#TASK" value
#    there is no done button:
    And click on the "Done" button

      # add data mapper step
    Then check visibility of page "Add to Integration"
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create mapping from "first_name" to "TASK"

    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "CRUD1-read-update E2E"
    And click on the "Publish" button
    Then check visibility of "CRUD1-read-update E2E" integration details
    Then navigate to the "Integrations" page
    Then wait until integration "CRUD1-read-update E2E" gets into "Published" state

    Then validate that all todos with task "Joe" have value completed "1", period in ms: "5000"


#
#  2. select - insert
#
  @db-connection-crud-2-read-create
  Scenario: Read & create operations
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Done" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
#    @wip - bug to be reported, wrong is: (:#TASK, 2). (:#TASK, :#MASK) is OK
    Then fill in invoke query input with "INSERT INTO TODO(task, completed) VALUES (:#TASK, 2)" value
    And click on the "Done" button

      # add data mapper step
    Then check visibility of page "Add to Integration"
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create mapping from "first_name" to "TASK"

#    And scroll "top" "right"
    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "CRUD2-read-create E2E"
    And click on the "Publish" button
    #@wip there is no more h1 label with integration name there, syndesis #430
    Then check visibility of "CRUD2-read-create E2E" integration details
    Then navigate to the "Integrations" page
    Then wait until integration "CRUD2-read-create E2E" gets into "Published" state

    Then validate that all todos with task "Joe" have value completed "2", period in ms: "5000"

#
#  3. select - delete
#
  @db-connection-crud-3-read-delete
  Scenario: Read & delete operations
    Then inserts into "todo" table
      | Joe |
    Then inserts into "todo" table
      | Jimmy |

    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Done" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT WHERE first_name = 'Joe'" value
    Then fill in period input with "10" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "DELETE FROM TODO WHERE task = :#TASK" value
    And click on the "Done" button

    # add data mapper step
    Then check visibility of page "Add to Integration"
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
#    And sleep for "16000" ms
    And create mapping from "first_name" to "TASK"

    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "CRUD3-read-delete E2E"
    And click on the "Publish" button
    #@wip there is no more h1 label with integration name there, syndesis #430
    Then check visibility of "CRUD3-read-delete E2E" integration details
    Then navigate to the "Integrations" page
    Then wait until integration "CRUD3-read-delete E2E" gets into "Published" state

    Then validate that number of all todos with task "Joe" is "0", period in ms: "5000"
    Then validate that number of all todos with task "Jimmy" is "1", period in ms: "1"

#
#  4. select - create (via buildin procedure)
#
  @db-connection-crud-4-read-update-inbuilt
  Scenario: Read & create operations on stored procedure
      # INSERT INTO CONTACT(first_name, last_name, company, lead_source) VALUES('Josef','Stieranka','Istrochem','db');


    When inserts into "contact" table
      | Josef | Stieranka | Istrochem | db |

    Then navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Done" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "10" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke Stored Procedure" integration action
    And select "add_lead" from "procedureName" dropdown
    And click on the "Done" button

      # add data mapper step
    Then check visibility of page "Add to Integration"
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui

    Then create data mapper mappings
      | company     | company             |
      | last_name   | first_and_last_name |
      | lead_source | lead_source         |

#    And scroll "top" "right"
    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "CRUD4-read-create-inbuilt E2E"
    And click on the "Publish" button
    Then check visibility of "CRUD4-read-create-inbuilt E2E" integration details
    Then navigate to the "Integrations" page
    Then wait until integration "CRUD4-read-create-inbuilt E2E" gets into "Published" state
    Then validate add_lead procedure with last_name: "Stieranka", company: "Istrochem", period in ms: "10000"



#
#  5. builtin sql query checker
#
  @db-connection-5-sqlquery-checker
  Scenario: Sql query checker
    Then inserts into "todo" table
      | Joe |
    Then inserts into "todo" table
      | Jimmy |

    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Done" button is "Disabled"
#    wrong query:
    Then fill in periodic query input with "SELECT * FROM CONTACT-A" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Done" button
    And check visibility of alert notification
    #@wip time_unit_id is not yet available

    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    And click on the "Done" button

    # select postgresDB connection as 'to' point
    #Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    Then fill in invoke query input with "UPDATE TODO-A SET completed=1 WHERE task like '%:#TASK%'" value
    And click on the "Done" button
    And check visibility of alert notification
    Then fill in invoke query input with "DELETE FROM TODO-A WHERE task like '%:#TASK%'" value
    And click on the "Done" button
    And check visibility of alert notification
    Then fill in invoke query input with "INSERT INTO TODO-A(task) VALUES(:#TASK)" value
    And click on the "Done" button
    And check visibility of alert notification
#    correct one:
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "DELETE FROM TODO WHERE task = :#TASK" value
    And click on the "Done" button

      # add data mapper step
    Then check visibility of page "Add to Integration"
    When click on the "Add a Step" button
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
#    And sleep for "5000" ms
    And create mapping from "first_name" to "TASK"

    And click on the "Done" button
    And click on the "Publish" button
    And set integration name "DB Connection 5 SQL query checker E2E"
    And click on the "Publish" button
    #@wip there is no more h1 label with integration name there, syndesis #430
    Then check visibility of "DB Connection 5 SQL query checker E2E" integration details
    Then navigate to the "Integrations" page
    Then wait until integration "DB Connection 5 SQL query checker E2E" gets into "Published" state

    Then validate that number of all todos with task "Joe" is "0", period in ms: "5000"
    Then validate that number of all todos with task "Jimmy" is "1", period in ms: "1"
