package edu.umn.cs.spoton.front;

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import edu.umn.cs.spoton.front.generators.aws.states.StatisticsManager;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.util.ArrayList;

public class UserConfig {

  ArrayList<String> bucketNames = new ArrayList<>();
  ArrayList<DynamoDBInfo> dynamoDBInfos = new ArrayList<>();

  public UserConfig(FileType[] allowedFileTypes, boolean isDebugMode,
      boolean allowErroneousGeneration) {
    if (allowedFileTypes != null && allowedFileTypes.length > 0)
      InternalConfig.getInstance().setAllowedFileTypes(allowedFileTypes);
    InternalConfig.getInstance().setDebugModeOn(isDebugMode);
    InternalConfig.getInstance().setAllowErroneousGeneration(allowErroneousGeneration);
  }


  public void addBucket(String bucketName) {
    bucketNames.add(bucketName);
  }

  public void addDynamoDB(String tableName, String primaryKeyName,
      ScalarAttributeType primaryKeyType,
      String rangeKeyName, ScalarAttributeType rangeKeyType) {
    dynamoDBInfos.add(
        new DynamoDBInfo(tableName, primaryKeyName, primaryKeyType, rangeKeyName, rangeKeyType));
  }

  public void setupEnv() {
    InternalConfig.getInstance().setupEnv(bucketNames, dynamoDBInfos);
  }

  public void resetEnv() {
    InternalConfig.getInstance().resetEnv();
  }

  public int getTestcaseCount() {
    return StatisticsManager.testCasesCount;
  }

  public int getNoFileExtensionCount() {
    return StatisticsManager.noFileExtensionCount;
  }

  public String getFUZZ_DIR() {
    return InternalConfig.getInstance().LOCAL_FUZZDIR;
  }


  public static class DynamoDBInfo {

    public String tableName;
    public String primaryKeyName;
    public ScalarAttributeType primaryKeyType;
    public String rangeKeyName;
    public ScalarAttributeType rangeKeyType;

    public DynamoDBInfo(String tableName, String primaryKeyName, ScalarAttributeType primaryKeyType,
        String rangeKeyName, ScalarAttributeType rangeKeyType) {
      this.tableName = tableName;
      this.primaryKeyName = primaryKeyName;
      this.primaryKeyType = primaryKeyType;
      this.rangeKeyName = rangeKeyName;
      this.rangeKeyType = rangeKeyType;
    }
  }

}
