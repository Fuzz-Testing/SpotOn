package edu.umn.cs.spoton.front.generators.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import java.util.Optional;

//Defines a record in a dynamodb table.
public class Attribute {

    String attributeName;
    ScalarAttributeType attributeType;
    Optional<KeyType> keyType;
    Object attributeValue;

    public Attribute(String attributeName, ScalarAttributeType attributeType, KeyType keyType, Object attributeValue) {
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        this.keyType = Optional.ofNullable(keyType);
        this.attributeValue = attributeValue;
    }
}
