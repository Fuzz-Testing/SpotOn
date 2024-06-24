package edu.umn.cs.spoton.front.generators.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.string.AlphaStringGenerator;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.DynamoDbEnv;
import edu.umn.cs.spoton.front.generators.misc.CalendarStrGenerator;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemGenerator extends Generator<Item> {

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();
  private Table currentTable;

  public ItemGenerator() {
    super(Item.class);
  }

  @Override
  public Item generate(SourceOfRandomness r, GenerationStatus status) {
    return generateItem(r, status);
  }


  public Item generateItem(SourceOfRandomness r, GenerationStatus s) {
    List<Object[]> keys = DynamoDbEnv.getInstance().findPrimaryAndRangeKeys(currentTable);
    keys.removeIf(Objects::isNull);
    assert keys.size() == 1 || keys.size() == 2 : "dynamodb must have 1 or 2 keys only. failing.";
    List<Attribute> newItem = new ArrayList<>();
//        for (Object[] key : keys)
    for (int i = 0; i < keys.size(); i++) {
      Object[] key = keys.get(i);
      if (key != null) {
        KeyType keyType = i == 0 ? KeyType.HASH : KeyType.RANGE;
        Object itemVal = null;
        while (itemVal == null || (itemVal instanceof String && ((String) itemVal).isEmpty())) {
          itemVal = generateValueForType((ScalarAttributeType) key[1], r, s);
        }
        newItem.add(new Attribute((String) key[0], (ScalarAttributeType) key[1], keyType, itemVal));
      }
    }

    //number of attributes after the keys
    int numberOfAttributes = Math.max(
        geometricDistribution.sampleWithMean(InternalConfig.getInstance().MEAN_ATTRIBUTES, r) - 1,
        InternalConfig.getInstance().ZERO_LOWERBOUND);

    for (int i = 0; i < numberOfAttributes; i++) {
      String attributeName = null;
      //making sure we are not creating a null or an empty attribute name to the dynamodb.
      while (attributeName == null || attributeName.isEmpty()) {
//          attributeName = Config.stringDictionaryOn ? Config.stringDictionaryGenerator.generate(r, s) :
//              gen().make(AlphaStringGenerator.class).generate(r, s);}
        attributeName = gen().make(AlphaStringGenerator.class).generate(r, s);
      }
      ScalarAttributeType attributeType = r.choose(ScalarAttributeType.values());
      Object attributeValue = generateValueForType(attributeType, r, s);
      newItem.add(new Attribute(attributeName, attributeType, null, attributeValue));
    }
    return makeItemFromRecord(newItem);
  }

  //Creates an item by using the first to elements to be the primary and the rqnge key, if the range key exits. Then it populates the remaining attributes with their values using the appendRemainingAttributes.
  private Item makeItemFromRecord(List<Attribute> attributes) {
    Attribute primaryKey = attributes.get(0);

    assert primaryKey.keyType.isPresent()
        && primaryKey.keyType.get() == KeyType.HASH : "primary key problem. failing";

    boolean hasRangeKey = attributes.size() >= 2 && attributes.get(1).keyType.isPresent()
        && attributes.get(1).keyType.get() == KeyType.RANGE;

    int nominalIndexBegin = !hasRangeKey ? 1 : 2;

    Item item = null;
    if (hasRangeKey) {
      Attribute rangeKey = attributes.get(1);
      item = new Item().withPrimaryKey(primaryKey.attributeName, primaryKey.attributeValue,
                                       rangeKey.attributeName, rangeKey.attributeValue);
    } else
      item = new Item().withPrimaryKey(primaryKey.attributeName, primaryKey.attributeValue);

    appendRemainingAttributes(item, nominalIndexBegin,
                              attributes); //has the side effect of changing the item.

    return item;
  }

  private void appendRemainingAttributes(Item item, int nominalIndexBegin,
      List<Attribute> attributes) {
    for (int i = nominalIndexBegin; i < attributes.size(); i++) {
      Attribute attr = attributes.get(i);
      switch (attr.attributeType) {
        case S:
          item.withString(attr.attributeName, (String) attr.attributeValue);
          break;
        case N:
          item.withNumber(attr.attributeName, (Number) attr.attributeValue);
          break;
        case B:
          item.withBoolean(attr.attributeName, (boolean) attr.attributeValue);
          break;
      }
    }
  }

  //generates a new random value based on the type of the attribute.
  private Object generateValueForType(ScalarAttributeType scalarType,
      SourceOfRandomness r,
      GenerationStatus status) {
    switch (scalarType) {
      case S:
        boolean generateDateVal = r.nextFloat() < 0.25;//r.nextBoolean();
        String str;
        if (generateDateVal) {
          str = gen().make(CalendarStrGenerator.class).generate(r, status);
          return str;
        } else {
          FreshOrConstantGenerator.setUseAnyStringGenerator(true);
          FreshOrConstantGenerator dictionaryBackedStrGen = gen().make(
              FreshOrConstantGenerator.class);
          str = dictionaryBackedStrGen.generate(r, status);
          System.out.println("string non-date selection, " + str);
          return str;
        }
      case N:
//        int value = geometricDistribution.sampleWithMean(2, r);
        int value = r.nextInt(InternalConfig.getInstance().NUMBER_LOWER_UPPER_BOUND[0],
                              InternalConfig.getInstance().NUMBER_LOWER_UPPER_BOUND[1]);
        return value;
      case B:
        boolean boolVal = r.nextBoolean();
        return boolVal;
    }
    assert false : "unexpect type for attribute. failing";
    return null;
  }

  public ItemGenerator setCurrentTable(Table table) {
    this.currentTable = table;
    return this;
  }
}
