package gss.calendar.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.jsoup.Jsoup;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @program: calendar
 * @description:
 * @author: Yidan
 * @create: 2023-11-21 11:19
 **/

public class SyncEventInfo {
  /**
   * send get to neu
   *
   * @param urlParam
   * @return
   * @throws IOException
   */
  private static String sendGet(String urlParam) throws IOException {
    // instantiate httpclient
    HttpClient httpClient = new HttpClient();
    // set socket timeout
    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(15000);
    // initiate get
    GetMethod getMethod = new GetMethod(urlParam);
    // initiate timeout of get method
    getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 60000);
    // set request header
    getMethod.addRequestHeader("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
    getMethod.addRequestHeader("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
    getMethod.addRequestHeader("Connection", "keep-alive");
    getMethod.addRequestHeader("Cookie", getResourceAsString("/cookie"));
    getMethod.addRequestHeader("Connection", "keep-alive");
    getMethod.addRequestHeader("Sec-Fetch-Dest", "document");
    getMethod.addRequestHeader("Sec-Fetch-Mode", "navigate");
    getMethod.addRequestHeader("Sec-Fetch-Site", "none");
    getMethod.addRequestHeader("Sec-Fetch-User", "?1");
    getMethod.addRequestHeader("Upgrade-Insecure-Requests", "1");
    getMethod.addRequestHeader("User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
    getMethod.addRequestHeader("sec-ch-ua",
        "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"");
    getMethod.addRequestHeader("sec-ch-ua-mobile", "?0");
    getMethod.addRequestHeader("sec-ch-ua-platform", "\"macOS\"");
    httpClient.executeMethod(getMethod);

    return getMethod.getResponseBodyAsString();
  }

  /**
   * get resource string
   *
   * @param filePath
   * @return
   */
  private static String getResourceAsString(String filePath) {
    InputStream inputStream = SyncEventInfo.class.getResourceAsStream(filePath);

    if (inputStream != null) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  /**
   * decode html response
   *
   * @param s
   * @return
   */
  private static List<String> decodeHtml(String s) {
    List<String> res = new ArrayList<>();
    Document doc = Jsoup.parse(s);

    System.out.println(doc);
    Element appointmentDropdown = doc.selectFirst("a#appointmentDropdown");

    if (appointmentDropdown != null) {
      Elements appointments = appointmentDropdown.nextElementSibling().select("a.dropdown-item");

      if (!appointments.isEmpty()) {
        for (Element appointment : appointments) {
          res.add(appointment.text());
        }
      } else {
        System.out.println("No appointments found.");
      }
    } else {
      System.out.println("My Appointments section not found.");
    }
    // Define time zones for EST and PT
    ZoneId estZone = ZoneId.of("America/New_York");
    ZoneId ptZone = ZoneId.of("America/Los_Angeles");

    // Custom formatter for the input timestamps with day and comma
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendPattern("EEEE, MMM. d: h:mm a")
        .parseDefaulting(java.time.temporal.ChronoField.YEAR, LocalDateTime.now().getYear());

    DateTimeFormatter formatter = builder.toFormatter(Locale.US);

    List<String> ptTimestamps = new ArrayList<>();

    for (String timestamp : res) {
      LocalDateTime localDateTime = LocalDateTime.parse(timestamp, formatter);

      // Convert to EST ZonedDateTime
      ZonedDateTime estDateTime = ZonedDateTime.of(localDateTime, estZone);

      // Convert to PT ZonedDateTime
      ZonedDateTime ptDateTime = estDateTime.withZoneSameInstant(ptZone);

      // Format the PT timestamp
      String ptTimestamp = ptDateTime.format(formatter);
      ptTimestamps.add(ptTimestamp);
    }

    return ptTimestamps;
  }

  public static List<Event> createEvents() throws ParseException, IOException {
    List<String> strings = decodeHtml(sendGet("https://neu.mywconline.net/schedule/calendar"));
    List<Event> res = new ArrayList<>();
    // Get the current year
    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM. d, yyyy, h:mm a");
    // Set the timezone to Pacific Time
    dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

    for (String str : strings) {
      String[] parts = str.split(": ");
      String dateStr = parts[0] + ", " + currentYear + ", " + parts[1];
      Date startDate = dateFormat.parse(dateStr);
      // Adding 1 hour to start date for the event duration
      Date endDate = new Date(startDate.getTime() + (60 * 60 * 1000));

      // Create an event.
      Event event = new Event().setSummary("ITC Tutoring").setDescription("virtualMeeting");

      // Set event start and end time (e.g., start and end date/time in ISO format).
      DateTime startTime = new DateTime(startDate, TimeZone.getTimeZone("America/Los_Angeles"));
      DateTime endTime = new DateTime(endDate, TimeZone.getTimeZone("America/Los_Angeles"));
      event.setStart(new EventDateTime().setDateTime(startTime));
      event.setEnd(new EventDateTime().setDateTime(endTime));
      res.add(event);
    }
    return res;
  }

}