@timer-connector
Feature: Timer connector

  Background:
    Given clean application state
    And deploy HTTP endpoints
    And create HTTP connection

  Scenario: Simple Timer
    When add "timer" endpoint with connector id "timer" and "timer-action" action and with properties:
      | action       | period |
      | timer-action | 1000   |

    And create HTTP "GET" step
    And create integration with name: "timer-to-http"
    And wait for integration with name: "timer-to-http" to become active

    Then verify that after "2.5" seconds there were "2" calls


  Scenario: Cron Timer
    When add "timer" endpoint with connector id "timer" and "timer-chron" action and with properties:
      | action     | cron         |
      | timer-cron | 0/1 * * * * ? |
    And create HTTP "GET" step
    And create integration with name: "cron-timer-to-http-1"
    And wait for integration with name: "cron-timer-to-http-1" to become active

    Then verify that after "60" seconds there were "1" calls
