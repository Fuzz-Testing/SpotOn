package edu.berkeley.cs.jqf.generators.structured.date;

import static java.util.GregorianCalendar.DAY_OF_MONTH;
import static java.util.GregorianCalendar.HOUR;
import static java.util.GregorianCalendar.MILLISECOND;
import static java.util.GregorianCalendar.MINUTE;
import static java.util.GregorianCalendar.MONTH;
import static java.util.GregorianCalendar.SECOND;
import static java.util.GregorianCalendar.YEAR;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.time.Year;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CalendarGenerator extends Generator<GregorianCalendar> {

  //Defines the mean of years that are way from a calendar selection.
  final int MEAN_OF_YEARS_AWAY = 5;

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();

  public CalendarGenerator() {
    super(GregorianCalendar.class); // Register the type of objects that we can create
  }

  // This method is invoked to generate a single test case
  @Override
  public GregorianCalendar generate(SourceOfRandomness random, GenerationStatus __ignore__) {
    // Initialize a calendar object
    GregorianCalendar cal = new GregorianCalendar();
    cal.setLenient(true); // This allows invalid dates to silently wrap (e.g. Apr 31 ==> May 1).

    // Randomly pick a day, month, and year
    cal.set(DAY_OF_MONTH, random.nextInt(31) + 1); // a number between 1 and 31 inclusive
    cal.set(MONTH, random.nextInt(12) + 1); // a number between 1 and 12 inclusive
    Integer yearsAway;
    yearsAway = geometricDistribution.sampleWithMean(MEAN_OF_YEARS_AWAY, random) - 1;
    if (random.nextFloat() > 0.25)
      cal.set(YEAR, Year.now().getValue() - yearsAway);
    else
      cal.set(YEAR, Year.now().getValue() + yearsAway);

    // Optionally also pick a time
    if (random.nextBoolean()) {
      cal.set(HOUR, random.nextInt(24));
      cal.set(MINUTE, random.nextInt(60));
      cal.set(SECOND, random.nextInt(60));
      cal.set(MILLISECOND, random.nextInt(1000));
    } else { //avoid using system time in this case, supply constants
      cal.set(HOUR, 0);
      cal.set(MINUTE, 0);
      cal.set(SECOND, 0);
      cal.set(MILLISECOND, 0);
    }

    // Let's set a timezone
    // First, get supported timezone IDs (e.g. "America/Los_Angeles")
    String[] allTzIds = TimeZone.getAvailableIDs();

    // Next, choose one randomly from the array
    String tzId = random.choose(allTzIds);
    TimeZone tz = TimeZone.getTimeZone(tzId);

    // Assign it to the calendar
    cal.setTimeZone(tz);

    // Return the randomly generated calendar object
    return cal;
  }
}