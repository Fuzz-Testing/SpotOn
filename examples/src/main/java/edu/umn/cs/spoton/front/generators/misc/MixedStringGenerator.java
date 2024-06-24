package edu.umn.cs.spoton.front.generators.misc;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.string.AlphaStringGenerator;
import edu.berkeley.cs.jqf.generators.string.AsciiStringGenerator;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.InternalConfig.StringGenerator;

/**
 * Not used anymore
 * used to create different forms of a string for contents of a text file. note that this uses a
 * specific generation status to control the mean of the lines generated in the text file to be
 * around 65 bytes.
 */
public class MixedStringGenerator extends Generator<String> {


  public MixedStringGenerator() {
    super(String.class);
  }

  @Override
  public String generate(SourceOfRandomness r, GenerationStatus status) {
//    GenerationStatus genStatus = new FlexibleNonTrackingGenerationStatus(r);
    boolean generateDateVal = r.nextInt(0, 5) == 0;
    if (generateDateVal) {
      String str = gen().make(CalendarStrGenerator.class).generate(r, status);
      return str;
    }
    StringGenerator strGenType = r.choose(InternalConfig.getInstance().ALL_STRING_GENERATORS);
    String output = "";
    switch (strGenType) {
      case ASCII:
        output = gen().make(AsciiStringGenerator.class).generate(r, status);
        break;
      case ALPHA:
        output = gen().make(AlphaStringGenerator.class).generate(r, status);
        break;
      case CODE_POINT:
        output = gen().type(String.class).generate(r, status);
        break;
      case EMPTY: //do nothing, we already have output being the empty string.
        break;
    }

//    Config.stringDictionaryGenerator.setFallbackGenerator(output);
//    return Config.stringDictionaryGenerator.generate(r, genStatus);
//    System.out.println("string non-date selection, " + output);
    return output;
  }
}
