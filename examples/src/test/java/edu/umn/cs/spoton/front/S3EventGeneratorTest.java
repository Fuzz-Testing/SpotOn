package edu.umn.cs.spoton.front;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.event.S3EventGenerator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class S3EventGeneratorTest {

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    InternalConfig.getInstance().setLocalStackEndpoint("http://localhost:8010");
    S3EventGenerator.bucketTest = S3Env.buckets.get(0);
  }

  @Fuzz
  public void generatorTest(@From(S3EventGenerator.class) S3Event event) {
    assert event.getRecords().size() > 0 : "failed to generate s3 events";
//    S3Entity s3Entity = event.getRecords()
//        .get(0).getS3();
//    System.out.println(
//        "bucket = " + s3Entity.getBucket().getName() + ", key = " + s3Entity.getObject().getKey());
  }

}
