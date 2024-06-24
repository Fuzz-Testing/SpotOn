package edu.umn.cs.spoton.front.generators.aws.dynamodb;


import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.DynamoDbEnv;
import edu.umn.cs.spoton.front.generators.aws.states.StatisticsManager;
import java.time.Duration;
import java.time.Instant;

public class DynamoDbGenerator extends Generator<Table> {

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();

  public DynamoDbGenerator() {
    super(Table.class);
  }

  @Override
  public Table generate(SourceOfRandomness r, GenerationStatus status) {
    int tablesCount = 0;
    Table table = null;
    IteratorSupport<Table, ListTablesResult> dynamoItr = DynamoDbEnv.tables.iterator();
    while (dynamoItr.hasNext()) {
      table = dynamoItr.next();
      int numberOfItems = Math.max(1, geometricDistribution.sampleWithMean(
          InternalConfig.getInstance().MEAN_OF_ROWS,
          r)); //generating at least a single row
      for (int i = 0; i < numberOfItems; i++) {
        Instant beforeDate = Instant.now();
        Item item = gen().make(ItemGenerator.class).setCurrentTable(table)
            .generate(r, status);
        table.putItem(item);
        Instant afterDate = Instant.now();
        long diff = Duration.between(beforeDate, afterDate).toMillis();
        StatisticsManager.addDynamoStates(item, diff);
      }
      int counter = 0;
      while (table.describe().getItemCount() == 0) {
        try {
          if (counter < 12) {
            counter++;
            Thread.sleep(200); //sleeping until we can see the populated data
            System.out.println("sleeping until data is populated.");
          } else {
            assert false :
                "waited for 40 seconds but dynamodb table still empty. Number of items attempted to be inserted = "
                    + numberOfItems + "failing.";
          }

        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      tablesCount++;
    }
    assert tablesCount <= 1 : "states are not supported for multiple tables yet.";

    return table;
  }
}
