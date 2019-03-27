# @sustainer: mcada@redhat.com

@ui
@salesforce
@database
@datamapper
@import
@export
@integrations-import-export
Feature: Integration - Import Export

  Background: Clean application state
    Given clean application state
    And reset content of "contact" table
    And log into the Syndesis

#
#  1. integration-import both methods, for investigation one of them use integration-import-export_backup.feature
#
  @integration-import-export-classic-and-dnd
  Scenario: Import and export both flows

    Given created connections
      | Slack | QE Slack  | QE Slack  | SyndesisQE Slack test |

    # create integration
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    And check visibility of visual integration editor
    Then check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    Then check "Next" button is "Disabled"
    And fill in periodic query input with "SELECT company FROM CONTACT ORDER BY lead_source DESC limit(1)" value
    And fill in period input with "60" value
    And select "Seconds" from sql dropdown
    And click on the "Next" button

    Then check that position of connection to fill is "Finish"

    # select slack connection
    When select the "QE Slack" connection
    And select "Channel" integration action
    And fill in values
      | Channel | test |

    And click on the "Done" button

    # add data mapper step
    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create mapping from "company" to "message"
    And click on the "Done" button

    # finish and save integration
    And click on the "Save" button
    And set integration name "Integration_import_export_test"
    And publish integration
    Then Integration "Integration_import_export_test" is present in integrations list

    When inserts into "CONTACT" table
      | Lorem | Ipsum | Red Hat | e_db |

    # wait for integration to get in active state
    And wait until integration "Integration_import_export_test" gets into "Running" state

    Then check that last slack message equals "Red Hat" on channel "test"

    #Add a new contact
    When inserts into "CONTACT" table
      | Fedora | 28 | RH | f_db |

    # export the integration for import tests
    And select the "Integration_import_export_test" integration
    Then check visibility of "Integration_import_export_test" integration details
    And export the integraion

    # now we have exported integration, we can clean state and try to import
    Given clean application state
    And log into the Syndesis
    And navigate to the "Integrations" page
    And click on the "Import" button
    Then import integration "Integration_import_export_test"

    When navigate to the "Integrations" page
    Then Integration "Integration_import_export_test" is present in integrations list
    Then wait until integration "Integration_import_export_test" gets into "Stopped" state

    # check draft status after import
    When select the "Integration_import_export_test" integration
    And check visibility of "Stopped" integration status on Integration Detail page
    And sleep for jenkins delay or "3" seconds
    # start integration and wait for published state
    When click on the "Edit" button
    And publish integration

    Then Integration "Integration_import_export_test" is present in integrations list

    Then wait until integration "Integration_import_export_test" gets into "Running" state

    And check that last slack message equals "RH" on channel "test"

#
#  2. integration-import with drag'n'drop
#

    #Add a new contact
    When inserts into "CONTACT" table
      | RHEL | 7 | New RH | g_db |

    And delete the "Integration_import_export_test" integration
    #wait for pod to be deleted
    And sleep for "15000" ms
    And navigate to the "Integrations" page
    And click on the "Import" button
    Then drag exported integration "Integration_import_export_test" file to drag and drop area

    When navigate to the "Integrations" page
    Then Integration "Integration_import_export_test" is present in integrations list
    And wait until integration "Integration_import_export_test" gets into "Stopped" state

    # check draft status after import
    When select the "Integration_import_export_test" integration
    And check visibility of "Stopped" integration status on Integration Detail page
    And sleep for jenkins delay or "3" seconds
    # start integration and wait for active state
    When click on the "Edit" button
    And publish integration

    And sleep for jenkins delay or "3" seconds
    And navigate to the "Integrations" page
    And Integration "Integration_import_export_test" is present in integrations list

    Then wait until integration "Integration_import_export_test" gets into "Running" state

    And check that last slack message equals "New RH" on channel "test"

#
#  2. integration-import from different syndesis instance
#
  @integration-import-from-different-instance
  Scenario: Import from different syndesis instance

    #Add a new contact
    When inserts into "CONTACT" table
      | RedHat | HatRed | RHEL | h_db |

    And navigate to the "Integrations" page
    And click on the "Import" button
    # import from resources TODO
    Then import integration from relative file path "src/test/resources/integrations/Imported-integration-another-instance-export.zip"

    When navigate to the "Connections" page
    And click on the "Edit" kebab menu button of "QE Slack"
    Then check visibility of "QE Slack" connection details

    When click on the "Edit" button
    And fill in "QE Slack" connection details from connection edit page
    And click on the "Validate" button
    Then check visibility of success notification
    And click on the "Save" button

    When navigate to the "Integrations" page

    #should be unpublished after import
    And select the "Integration_import_export_test" integration
    Then check visibility of "Stopped" integration status on Integration Detail page

    When click on the "Edit" button
    And publish integration

    And Integration "Integration_import_export_test" is present in integrations list

    # wait for integration to get in active state

    Then wait until integration "Integration_import_export_test" gets into "Running" state

    And check that last slack message equals "RHEL" on channel "test"
