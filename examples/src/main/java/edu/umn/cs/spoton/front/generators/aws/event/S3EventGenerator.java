package edu.umn.cs.spoton.front.generators.aws.event;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.Bucket;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.util.List;


public class S3EventGenerator extends
    Generator<com.amazonaws.services.lambda.runtime.events.S3Event> {


  //there is only one source of an event if we want to generate s3 event based notification.
  private static String EVENT_SOURCE = "aws:s3";

  //currently we assume a single event name onto s3 of putobject.
  private static String EVENT_NAME = "PutObject";
  private Bucket bucket = bucketTest != null ? bucketTest : null;

  public static Bucket bucketTest; //parameter that is set when running unit tests.

  public S3EventGenerator() {
    super(com.amazonaws.services.lambda.runtime.events.S3Event.class);
  }

  @Override
  public com.amazonaws.services.lambda.runtime.events.S3Event generate(SourceOfRandomness r,
      GenerationStatus status) {
    assert bucket != null : "bucket of the event cannot be null";
    S3NotificationRecordListGenerator s3NotificationRecordListGenerator = gen().make(
        S3NotificationRecordListGenerator.class);
    s3NotificationRecordListGenerator.setBucket(this.bucket);
    s3NotificationRecordListGenerator.setEventName(EVENT_NAME);
    s3NotificationRecordListGenerator.setEventSource(EVENT_SOURCE);
    s3NotificationRecordListGenerator.setBucketTest(bucketTest);
    List<S3EventNotificationRecord> records = s3NotificationRecordListGenerator.generate(
        r, status);
    return new S3Event(records);  }

  public void setBucket(Bucket bucket) {
    this.bucket = bucket;
  }
}
