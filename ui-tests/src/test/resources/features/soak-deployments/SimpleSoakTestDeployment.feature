@disabled
@Soak
Feature: Simple soak test

  Scenario: Prepare
    Given deploy AMQ broker and add accounts
    Given log into the Syndesis
    Given created connections
      | AMQ Message Broker   | AMQ            | AMQ   | AMQ connection    |

  Scenario Outline: Timer Publish to queue
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor

    When selects the "Timer" connection
    And select "Simple Timer" integration action
    And fill in values
      | period | 0.016 |

    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "AMQ" connection
    And select "Publish Messages" integration action

    And fill in values
      | Destination Name | queue-<number> |
      | Destination Type | Queue          |
    And click on the "Next" button
    And click on the "Done" button


    # final steps

    When click on the "Publish" button
    And set integration name "Timer2AMQ #<number>"
    And click on the "Publish" button

    And  check visibility of "Timer2AMQ #<number>" integration details
    And  navigate to the "Integrations" page
    Then  sleep for "120000" ms

    Examples:
      | number |
      | 1      |
      | 2      |
      | 3      |


  Scenario Outline: Read from Queue
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor

    When selects the "AMQ" connection
    And select "Subscribe for Messages" integration action
    And fill in values
      | Destination Name | queue-<number> |
      | Destination Type | Queue          |
    And click on the "Next" button
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "Log" connection
    And select "Simple Logger" integration action
    And click on the "Done" button

    # final steps
    When click on the "Publish" button
    And set integration name "AMQ2Log #<number>"
    And click on the "Publish" button

    And  check visibility of "AMQ2Log #<number>" integration details
    And  navigate to the "Integrations" page
    Then  sleep for "120000" ms

    Examples:
      | number |
      | 1      |
      | 2      |
      | 3      |

