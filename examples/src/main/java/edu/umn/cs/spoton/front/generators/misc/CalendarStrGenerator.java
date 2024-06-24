package edu.umn.cs.spoton.front.generators.misc;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.structured.date.CalendarGenerator;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

public class CalendarStrGenerator extends Generator<String> {

  private int currentYear;

  public CalendarStrGenerator() {
    super(String.class); // Register the type of objects that we can create
  }

  // This method is invoked to generate a single test case
  @Override
  public String generate(SourceOfRandomness r, GenerationStatus status) {
    GregorianCalendar cal = gen().make(CalendarGenerator.class).generate(r, status);

    ZonedDateTime date = cal.toZonedDateTime();
    DateTimeFormatter[] allDateTimeFormats = new DateTimeFormatter[]{
        DateTimeFormatter.ISO_LOCAL_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_OFFSET_DATE,
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ISO_OFFSET_TIME,
        DateTimeFormatter.ISO_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_ORDINAL_DATE,
        DateTimeFormatter.ISO_WEEK_DATE,
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.BASIC_ISO_DATE,
        DateTimeFormatter.RFC_1123_DATE_TIME};

    String dateStr = null;
    while (dateStr == null) {
      DateTimeFormatter dateFormate = r.choose(allDateTimeFormats);
      try {
        dateStr = date.format(dateFormate);
      } catch (Exception e) {
        //retry
      }
    }
    return dateStr;
  }
}