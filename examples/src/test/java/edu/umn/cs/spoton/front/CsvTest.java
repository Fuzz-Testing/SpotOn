package edu.umn.cs.spoton.front;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.CsvGenerator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class CsvTest {

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
  }

  @Fuzz
  public void generatorTest(@From(CsvGenerator.class) String str) {
    System.out.println(str);
  }

}
