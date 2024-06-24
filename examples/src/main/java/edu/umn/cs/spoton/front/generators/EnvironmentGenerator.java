package edu.umn.cs.spoton.front.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Generators;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.aws.EventWrapper;
import edu.umn.cs.spoton.front.generators.aws.dynamodb.DynamoDbGenerator;
import edu.umn.cs.spoton.front.generators.aws.s3.S3Generator;

public class EnvironmentGenerator extends Generator<EventWrapper> {

  public EnvironmentGenerator() {
    super(EventWrapper.class);
  }

  //this mutates the environment
  @Override
  public EventWrapper generate(SourceOfRandomness r, GenerationStatus status) {
    if (InternalConfig.getInstance().isDebugModeOn())
      System.out.println("before generate resources");
    S3Generator s3Generator = gen().make(S3Generator.class);
    s3Generator.generate(r, status);

    Generators g = gen();
    if (InternalConfig.getInstance().getDynamoDBEnv() != null) {
      DynamoDbGenerator dynamoDbGenerator = g.make(DynamoDbGenerator.class);
      dynamoDbGenerator.generate(r, status);
    }
    if (InternalConfig.getInstance().isDebugModeOn())
      System.out.println("after generate event");

    return new EventWrapper(s3Generator.getS3Event());
  }
}
