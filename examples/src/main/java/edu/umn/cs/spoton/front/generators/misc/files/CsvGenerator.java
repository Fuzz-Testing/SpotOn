package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.CalendarStrGenerator;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import java.util.ArrayList;
import java.util.List;

public class CsvGenerator extends Generator<String> {

  enum Types {STRING, INT, DATE, BOOLEAN}

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();

  public CsvGenerator() {
    super(String.class);
  }

  @Override
  public String generate(SourceOfRandomness r, GenerationStatus s) {
    int numOfRowsAfterHeader = Math.max(InternalConfig.getInstance().ZERO_LOWERBOUND,
                                        geometricDistribution.sampleWithMean(
                                            InternalConfig.getInstance().MEAN_OF_ROWS, r)
                                            - 1);
    int numOfColumns = Math.max(1,
                                geometricDistribution.sampleWithMean(
                                    InternalConfig.getInstance().MEAN_ATTRIBUTES, r)
                                    - 1);
    ArrayList<Types> columnTypes = selectColumnTypes(numOfColumns, r);
    String csvContent = generateRow(columnTypes, r, s) + "\n"; // header creation

    for (int i = 0; i < numOfRowsAfterHeader; i++) //rows after header creation
      csvContent += generateRow(columnTypes, r, s) + "\n";

    return csvContent;
  }

  /**
   * takes in a list of types, and returns a row in csv with the specified type
   */
  private String generateRow(List<Types> types, SourceOfRandomness r,
      GenerationStatus s) {
    int n = types.size();

    if (n == 1)
      return generateValueForType(types.get(0), r, s).toString();
    else if (n > 1) {
      String rowValue = "";
      for (int i = 0; i < n - 1; i++)
        rowValue = generateValueForType(types.get(i), r, s) + ",";
      rowValue += generateValueForType(types.get(n - 1), r, s);
      return rowValue;
    } else {
      assert false : "a row in csv file must have at least a single column value";
      return null;
    }
  }

  private ArrayList<Types> selectColumnTypes(int numOfColumns, SourceOfRandomness r) {
//    ArrayList<Types> typePool = new ArrayList<>();
    ArrayList<Types> types = new ArrayList<>();
//    for (int i = 0; i < PROPORTION_OF_INT; i++)
//      typePool.add(Types.INT);
//    for (int i = 0; i < PROPORTION_OF_STRING; i++)
//      typePool.add(Types.STRING);
//    for (int i = 0; i < PROPORTION_OF_DATE; i++)
//      typePool.add(Types.DATE);
    for (int i = 0; i < numOfColumns; i++)
      types.add(r.choose(Types.values()));

    return types;
  }

  //generates a new random value based on the type of the attribute.
  private Object generateValueForType(Types type, SourceOfRandomness r,
      GenerationStatus status) {
    Object value = null;
    switch (type) {
      case STRING:
        FreshOrConstantGenerator.setUseAnyStringGenerator(true);
        value = gen().make(
            FreshOrConstantGenerator.class).generate(r, status);
        break;
      case BOOLEAN:
        value = r.nextBoolean() ? "True" : "False";
        break;
      case INT:
        value = r.nextInt(InternalConfig.getInstance().NUMBER_LOWER_UPPER_BOUND[0],
                          InternalConfig.getInstance().NUMBER_LOWER_UPPER_BOUND[1]);
        break;
      case DATE:
        value = gen().make(CalendarStrGenerator.class).generate(r, status);
        break;
    }
    return value;
  }
}
