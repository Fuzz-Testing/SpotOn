package edu.umn.cs.spoton.front;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.event.S3EventGenerator;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class S3EventGeneratorErrornousTest {

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    InternalConfig.getInstance().setLocalStackEndpoint("http://localhost:8010");
    S3Env.getInstance().init(Collections.singletonList("my-bucket"));
    S3EventGenerator.bucketTest = S3Env.buckets.get(0);
    InternalConfig.getInstance().setAllowErroneousGeneration(false);
    InternalConfig.getInstance().setAllowNoExtFileTestApi(true);
    InternalConfig.getInstance().setAllowDifferentExtFileTest(false);
  }

  @Fuzz
  public void generatorTest(@From(S3EventGenerator.class) S3Event event) {
    assert event.getRecords().size() == 1 : "expected a single file in the event";

    S3EventNotificationRecord record = event.getRecords()
        .get(0);

    String srcBucket = record.getS3().getBucket().getName();

    String srcKey = record.getS3().getObject().getKey();
    assert !srcKey.contains(".") : "extension cannot exist in an erroneous file generation.";

    System.out.println(
        "bucket = " + srcBucket + ", key = " + srcKey);
  }
}
