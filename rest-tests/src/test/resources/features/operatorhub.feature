# @maintainer: mmuzikar@redhat.com

@operatorhub
Feature: Operator hub tests

  @operathorhub-smoke
  Scenario: Deploy from operatorhub and run an integration
    Given deploy Syndesis from OperatorHub
    And remove all records from table "TODO"
    And remove all records from table "CONTACT"

    Then inserts into "CONTACT" table
      | Josef_first  | Stieranka_first  | Syndesis-qe | db |
      | Josef_second | Stieranka_second | Syndesis-qe | db |

    When create start DB periodic sql invocation action step with query "SELECT * FROM CONTACT" and period "5000" ms
    And add a split step
    And create basic filter step for "last_name" with word "first" and operation "contains"
    And start mapper definition with name: "mapping 1"
    And MAP using Step 2 and field "/first_name" to "/<>/task"

    And create finish DB invoke sql action step with query "INSERT INTO TODO (task, completed) VALUES (:#task, 3)"
    Then create integration with name: "DB to DB smoke rest test"
    Then wait for integration with name: "DB to DB smoke rest test" to become active

    And sleep for jenkins delay or "15" seconds

    Then validate that number of all todos with task "Josef_first" is greater than "0"
    Then validate that number of all todos with task "Josef_second" is "0"