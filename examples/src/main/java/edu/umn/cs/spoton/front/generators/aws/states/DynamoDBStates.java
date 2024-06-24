package edu.umn.cs.spoton.front.generators.aws.states;

import com.amazonaws.services.dynamodbv2.document.Item;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DynamoDBStates implements StatesInterface {

  List<String> recordsStates = new ArrayList<>();
  @Override
  public void add(int testCasesCount, Object item, long time) {
    assert item instanceof Item;
    recordsStates.add(testCasesCount + "," + ((Item) item).toJSON() + "," + time);
  }

  @Override
  public void flush() { //flushing out the date in the recordsStates to a file
    String dynamoRecStr = "";
    for (String record : recordsStates)
      dynamoRecStr += record + "\n";

    FileManipulation.writeToFile(InternalConfig.getInstance().STATES_DIR +  File.separator + StatisticsManager.dynamoStatesFileName, dynamoRecStr);
    recordsStates.clear();
  }
}
