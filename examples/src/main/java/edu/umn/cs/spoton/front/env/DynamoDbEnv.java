package edu.umn.cs.spoton.front.env;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.*;
import edu.umn.cs.spoton.front.ServiceBuilder;
import edu.umn.cs.spoton.front.UserConfig.DynamoDBInfo;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamoDbEnv implements Environment {

  static DynamoDbEnv instance = new DynamoDbEnv();

  public static com.amazonaws.services.dynamodbv2.document.DynamoDB dynamoDbClient;

  public static TableCollection<ListTablesResult> tables;

  public static DynamoDbEnv getInstance() {
    return instance;
  }

  @Override
  public void init(Object dynamoDBInfoList) {
    dynamoDbClient = ServiceBuilder.prepareAmazonDynamoDB();
    tables = dynamoDbClient.listTables();
    dropTables();
    for (DynamoDBInfo dynamoDBInfo : (List<DynamoDBInfo>) dynamoDBInfoList)
      createTable(dynamoDBInfo.tableName,
                  new Object[]{dynamoDBInfo.primaryKeyName, dynamoDBInfo.primaryKeyType},
                  dynamoDBInfo.rangeKeyName != null ? new Object[]{dynamoDBInfo.rangeKeyName,
                      dynamoDBInfo.rangeKeyType} : null);

    tables = dynamoDbClient.listTables();
  }

  public void dropTables() {
    for (Table table : tables)
      dropTable(table);
  }

  @Override
  public void reset() {
    for (Table table : tables)
      resetTable(table);

    tables = dynamoDbClient.listTables(); //refresh set of tables.
  }

  //deleting all tables, this happens once when the system is starting fresh.
  public void deleteDynamoDB() {
    for (Table table : tables)
      table.delete();
    tables = null;
  }

  public void createTables(List<String> tableNames, List<Object[]> primaryKeys,
      List<Object[]> secondaryKeys) {
    assert (tableNames.size() == primaryKeys.size() && primaryKeys.size()
        == secondaryKeys.size()) : "inconsistency in dynamoDB initial state. Either no tables exists or same sizes exists.";

    if (tableNames == null)
      return;

    for (int i = 0; i < tableNames.size(); i++)
      createTable(tableNames.get(i), primaryKeys.get(i), secondaryKeys.get(i));
    tables = dynamoDbClient.listTables();
  }

  //reset a table by dropping and recreating it.
  private void resetTable(Table table) {

    List<Object[]> keys = findPrimaryAndRangeKeys(table);
    assert keys.size() == 1 || keys.size() == 2 : "dynamodb must have 1 or 2 keys only. failing.";
    Object[] primaryKey = keys.get(0);
    Object[] rangeKey = keys.get(1);

    dropTable(table);
    createTable(table.getTableName(), primaryKey, rangeKey);
  }

  public List<Object[]> findPrimaryAndRangeKeys(Table table) {
    TableDescription description = table.describe();

    List<KeySchemaElement> keys = description.getKeySchema();

    Object[] primaryKey = null;
    Object[] rangeKey = null;

    for (KeySchemaElement key : keys) {
      String name = key.getAttributeName();
      KeyType type =
          key.getKeyType().equals(KeyType.HASH.toString()) ? KeyType.HASH : KeyType.RANGE;
      if (type == KeyType.HASH) {
        primaryKey = new Object[]{name, findAttributeType(table, name)};
      } else {
        rangeKey = new Object[]{name, findAttributeType(table, name)};
      }
    }
    return Stream.of(primaryKey, rangeKey != null ? rangeKey : null).collect(Collectors.toList());
//    return List.of(primaryKey, rangeKey != null ? rangeKey : null);
  }

  private ScalarAttributeType findAttributeType(Table table, String name) {
    List<AttributeDefinition> attrs = table.getDescription().getAttributeDefinitions();
    for (AttributeDefinition attr : attrs) {
      if (attr.getAttributeName().equals(name))
        return ScalarAttributeType.fromValue(attr.getAttributeType());
    }
    assert false : "unable to find a type for an attribute. failing.";
    return null;
  }

  private void dropTable(Table table) {
    table.delete();
    try {
      table.waitForDelete();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  // using array of object here to hold, attribute name, and its ScalarAttributeType, since java standard library does not have pair class
  private void createTable(String tableName, Object[] primaryKey, Object[] rangeKey) {
    Table table = null;

    try {
      assert primaryKey != null : "primary key cannot be null. failing.";
      assert primaryKey.length == 2 && rangeKey == null
          || rangeKey.length == 2 : "unexpected length for keys. failing.";
      assert primaryKey[0] instanceof String
          && primaryKey[1] instanceof ScalarAttributeType : "unexpected type for primary key. failing.";
      assert rangeKey == null || rangeKey[0] instanceof String
          && rangeKey[1] instanceof ScalarAttributeType : "unexpected type for range key. failing.";
      if (rangeKey != null) {
        table = dynamoDbClient.createTable(tableName, Arrays.asList(
                                               new KeySchemaElement((String) primaryKey[0], KeyType.HASH), // Partition key
                                               new KeySchemaElement((String) rangeKey[0], KeyType.RANGE)), // Sort key
                                           Arrays.asList(
                                               new AttributeDefinition((String) primaryKey[0],
                                                                       (ScalarAttributeType) primaryKey[1]),
                                               new AttributeDefinition((String) rangeKey[0],
                                                                       (ScalarAttributeType) rangeKey[1])),
                                           new ProvisionedThroughput(10L, 10L));
      } else {
        table = dynamoDbClient.createTable(tableName, Arrays.asList(
                                               new KeySchemaElement((String) primaryKey[0], KeyType.HASH)), // Sort key
                                           Arrays.asList(
                                               new AttributeDefinition((String) primaryKey[0],
                                                                       (ScalarAttributeType) primaryKey[1])),
                                           new ProvisionedThroughput(10L, 10L));
      }
      table.waitForActive();
    } catch (Exception e) {
      System.err.println("Unable to create table: ");
      System.err.println(e.getMessage());
      throw new RuntimeException(e);
    }
//        return table;
  }
}
