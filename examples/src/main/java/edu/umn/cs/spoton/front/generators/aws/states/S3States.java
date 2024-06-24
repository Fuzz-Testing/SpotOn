package edu.umn.cs.spoton.front.generators.aws.states;

import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class S3States implements StatesInterface {

  List<String> recordsStates = new ArrayList<>();

  @Override
  public void add(int testCasesCount, Object item, long time) {
    assert item instanceof File : "unexpected type for s3 file";

//    try {
//      String lines = new String(Files.readAllBytes(Paths.get(((File) item).getPath())));
//      recordsStates.add(
//          testCasesCount + "," + ((File) item).getName() + "," + lines);
      recordsStates.add(
          testCasesCount + "," + ((File) item).getName() + "," + time);

//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public void flush() {
    String s3RecordStr = "";
    for (String record : recordsStates)
      s3RecordStr += record + "\n";

    FileManipulation.writeToFile(
        InternalConfig.getInstance().STATES_DIR +  File.separator + StatisticsManager.s3StatesFileName, s3RecordStr);
    recordsStates.clear();
  }
}
