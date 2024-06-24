package edu.umn.cs.spoton.front;

import static edu.umn.cs.spoton.front.env.S3Env.s3Client;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.s3.model.Bucket;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.ServiceBuilder;
import edu.umn.cs.spoton.front.UserConfig.DynamoDBInfo;
import edu.umn.cs.spoton.front.env.DynamoDbEnv;
import edu.umn.cs.spoton.front.env.S3Env;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnvTest {

  DynamoDB ddb = ServiceBuilder.prepareAmazonDynamoDB();

  @BeforeClass
  public static void settingParameters() {
    InternalConfig.resetInstance();
    InternalConfig.getInstance().setLocalStackEndpoint("http://localhost:8010");
  }

  @Test
  public void dynamoDbSetupTest() {
    DynamoDBInfo dynamoDBInfo = new DynamoDBInfo("Contracts", "contract_code",
                                                 ScalarAttributeType.S, "upload_date",
                                                 ScalarAttributeType.S);
    dropTables();
    assert getNumOfTables() == 0 : "no tables should exist after dropTable command";
    DynamoDbEnv.getInstance().init(Collections.singletonList(dynamoDBInfo));
    assert getNumOfTables() != 0 : "at least single table must exist";
  }

  @Test
  public void s3DbSetupTest() throws IOException {
    S3Env.getInstance().init(Collections.singletonList("my-bucket"));
    File sourceFile = new File(this.getClass().getResource("/FuzzDir/TestMe.txt").getPath());
    File destinationFile = new File(InternalConfig.getInstance().buildDir + "/FuzzDir/TestMe.txt");
    FileUtils.copyFile(sourceFile, destinationFile);
    ArrayList<File> localFiles = new ArrayList<File>();
    localFiles.add(destinationFile);
    Bucket bucket = S3Env.buckets.get(0);
    assert bucket != null : "bucket must exist";
    try {
      S3Env.getInstance().populateBatchToCloud(bucket, localFiles);
    } catch (IllegalArgumentException e) {
      throw e;
    }
    assert s3Client.listObjects(bucket.getName()).getObjectSummaries().size() != 0 :
        "bucket cannot be empty (" + bucket.getName() + ")";
    S3Env.getInstance().reset();
    assert s3Client.listObjects(bucket.getName()).getObjectSummaries().size() == 0 :
        "bucket not empty (" + bucket.getName() + ")";
  }

  public Integer getNumOfTables() {
    int count = 0;
    TableCollection<ListTablesResult> tables = ddb.listTables();
    for (Table table : tables)
      count++;

    return count;
  }

  public void dropTables() {
    TableCollection<ListTablesResult> tables = ddb.listTables();
    for (Table table : tables)
      dropTable(table);
  }

  private void dropTable(Table table) {
    table.delete();
    try {
      table.waitForDelete();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}