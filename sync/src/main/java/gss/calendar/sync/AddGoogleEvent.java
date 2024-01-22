package gss.calendar.sync;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * @program: calendar
 * @description:
 * @author: Yidan
 * @create: 2023-11-21 11:14
 **/

public class AddGoogleEvent {

  /**
   * Global instance of the Calendar client.
   */
  private static Calendar client;

  public static void main(String[] args) {
    try {
      List<String> scopes = Lists.newArrayList(CalendarScopes.CALENDAR);
      client = Utils.createCalendarClient(scopes);
      run();
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Adds a new event to the primary calendar.
   */
  private static void run() throws IOException, ParseException {
    // Create an event.
    List<Event> events = SyncEventInfo.createEvents();
    for (Event event : events) {
      // Insert the event.
      try {
        event = client.events().insert("primary", event).execute();
        System.out.println("Event created: " + event.getHtmlLink());
      } catch (GoogleJsonResponseException e) {
        e.printStackTrace();
      }
    }
  }
}