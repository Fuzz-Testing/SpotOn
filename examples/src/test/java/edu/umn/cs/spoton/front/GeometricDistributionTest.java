package edu.umn.cs.spoton.front;

import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import java.util.Random;
import org.junit.Test;

public class GeometricDistributionTest {

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();

  @Test
  public void pullFromDistribution() {
    SourceOfRandomness r = new SourceOfRandomness(new Random(2));
    int greaterThan20 = 0;
    for (int i = 0; i < 10000; i++) {
      int selection = geometricDistribution.sampleWithMean(InternalConfig.getInstance().MEAN_ATTRIBUTES, r);
      if (selection > 20)
        greaterThan20++;
      if (selection <= 0)
        System.out.println("matching 0 at iteration = " + i);
    }
    System.out.println("greaterThan20=" + greaterThan20);
  }

  //proves that we can on certain values obtain NaN result
  @Test
  public void geoSamplingTest() {
    double uniform = 100;
    double p = 1.0 / 5.0;
    double result = Math.ceil(Math.log(1 - uniform) / Math.log(1 - p));
    System.out.println("result = " + result);
  }
}
