package edu.umn.cs.spoton.front.generators.aws.states;

import com.amazonaws.services.dynamodbv2.document.Item;
import edu.umn.cs.spoton.front.InternalConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class StatisticsManager {


  public static int testCasesCount = 1;
  public static int noFileExtensionCount = 0;

  private static DynamoDBStates dynamoDBStates = new DynamoDBStates();
  private static S3States s3States = new S3States();
  static String benchmarkName = "";
  static String dynamoStatesFileName;
  static String s3StatesFileName;

  static {
    String projectDir = InternalConfig.getInstance().PROJECT_DIR;
    if (projectDir != null) {
      benchmarkName = projectDir.substring(0, projectDir.lastIndexOf("/"));
      benchmarkName =
          benchmarkName.substring(benchmarkName.lastIndexOf("/") + 1);
    }
    dynamoStatesFileName =
        File.separator + InternalConfig.getInstance().ENGINE + "_" + benchmarkName + "_dynamoStates.txt";
    s3StatesFileName = File.separator + InternalConfig.getInstance().ENGINE + "_" + benchmarkName + "_s3States.txt";
  }

  public static void makeStep() {
    dynamoDBStates.flush();
    s3States.flush();
    testCasesCount++;
  }

  public static void addDynamoStates(Item item, long time) {
    dynamoDBStates.add(testCasesCount, item, time);
  }

  public static void addS3States(Object item, long time) {
    assert item instanceof File : " unexpected type for s3 file";
    s3States.add(testCasesCount, item, time);
  }

  public static void printFilesInfoForLogging(ArrayList<File> localFiles) {
    System.out.println("number of files = " + localFiles.size());

    localFiles.stream().forEach(f -> {
      try {
        System.out.println(
            "filename = " + f.getAbsolutePath() + ", fileSize=" + Files.size(f.toPath()));
      } catch (IOException e) {
        assert false;
        throw new RuntimeException(e);
      }
    });
  }
}
