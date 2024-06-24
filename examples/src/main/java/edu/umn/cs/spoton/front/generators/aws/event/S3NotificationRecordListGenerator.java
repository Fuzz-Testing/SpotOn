package edu.umn.cs.spoton.front.generators.aws.event;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.Bucket;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.util.ArrayList;
import java.util.List;


public class S3NotificationRecordListGenerator extends
    Generator<List> {

  //there is only one source of an event if we want to generate s3 event based notification.
  String EVENT_SOURCE;

  String EVENT_NAME;
  private Bucket bucket;

  Bucket bucketTest;

  public S3NotificationRecordListGenerator() {
    super(List.class);
  }

  @Override
  public List<S3EventNotification.S3EventNotificationRecord> generate(SourceOfRandomness r,
      GenerationStatus status) {
    assert bucket != null : "bucket of the event cannot be null";
    List<S3EventNotification.S3EventNotificationRecord> records = new ArrayList<>();
    S3NotificationRecordGenerator s3NotificationRecordGenerator = gen().make(
        S3NotificationRecordGenerator.class);
    s3NotificationRecordGenerator.setBucket(this.bucket);
    s3NotificationRecordGenerator.setEventName(EVENT_NAME);
    s3NotificationRecordGenerator.setEventSource(EVENT_SOURCE);
    s3NotificationRecordGenerator.setBucketTest(bucketTest);
    S3EventNotificationRecord s3Record = s3NotificationRecordGenerator.generate(
        r, status);

    records.add(s3Record);
    return records;
  }

  public void setBucket(Bucket bucket) {
    this.bucket = bucket;
  }

  public void setEventSource(String eventSource) {
    EVENT_SOURCE = eventSource;
  }

  public void setEventName(String eventName) {
    EVENT_NAME = eventName;
  }

  public void setBucketTest(Bucket bucketTest) {
    this.bucketTest = bucketTest;
    if(bucketTest!=null)
      bucket = bucketTest;  }
}
