package io.syndesis.qe.bdd.validation;

import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.Event;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import io.syndesis.qe.utils.GoogleCalendarUtils;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class GoogleCalendarValidationSteps {
    @Autowired
    private GoogleCalendarUtils gcu;

    @Then("verify that event \"([^\"]*)\" with description \"([^\"]*)\" exists in calendar \"([^\"]*)\" using account \"([^\"]*)\"")
    public void verifyEventExists(String eventSummary, String eventDescription, String calendarName, String googleAccount) throws IOException {
        Calendar c = gcu.getPreviouslyCreatedCalendar(googleAccount, calendarName);
        Event e = gcu.getEventBySummary(googleAccount,c.getId(), eventSummary);
        Assert.assertNotNull(String.format("Event with summary \"%s\" does not exist in calendar %s", eventSummary, calendarName), e);
        Assert.assertEquals("Summary of the event does not match expected value.", eventSummary, e.getSummary());
        Assert.assertEquals("Description of the event does not match expected value.", eventDescription, e.getDescription());
    }

    @And("^verify that event \"([^\"]*)\" exists in calendar \"([^\"]*)\" using account \"([^\"]*)\"$")
    public void verifyThatEventExistsInCalendarUsingAccount(String eventSummary, String calendarName, String googleAccount) throws Throwable {
        Calendar c = gcu.getPreviouslyCreatedCalendar(googleAccount, calendarName);
        Event e = gcu.getEventBySummary(googleAccount,c.getId(), eventSummary);
        Assert.assertNotNull(String.format("Event with summary \"%s\" does not exist in calendar %s", eventSummary, calendarName), e);
        Assert.assertEquals("Summary of the event does not match expected value.", eventSummary, e.getSummary());
    }
}
