package edu.umn.cs.spoton.front.generators.aws.event;

import static com.amazonaws.auth.profile.internal.ProfileKeyConstants.REGION;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.model.Bucket;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;


public class S3NotificationRecordGenerator extends
    Generator<S3EventNotification.S3EventNotificationRecord> {


  //there is only one source of an event if we want to generate s3 event based notification.
  private String EVENT_SOURCE;

  private String EVENT_NAME;
  private Bucket bucket;

  Bucket bucketTest; //parameter that is set when running unit tests.

  public S3NotificationRecordGenerator() {
    super(S3EventNotification.S3EventNotificationRecord.class);
  }

  @Override
  public S3EventNotification.S3EventNotificationRecord generate(SourceOfRandomness r,
      GenerationStatus status) {
    assert bucket != null : "bucket of the event cannot be null";

    String fileName = generateFileName(r, status);
    return constructS3EventNotificationRecord(fileName);
  }

  private String generateFileName(SourceOfRandomness r, GenerationStatus status) {
    FileType fileType = r.choose(InternalConfig.getInstance().getAllowedFileTypes());
    FreshOrConstantGenerator.setUseAnyStringGenerator(
        false); //we want to use alpha names for filenames
    String initialFileName = null;
    while (initialFileName == null || initialFileName.isEmpty()
        || FileTypeUtil.isRestrictedFileName(
        initialFileName)) {
      initialFileName = gen().make(FreshOrConstantGenerator.class).generate(r, status);
      initialFileName = FileTypeUtil.removeIllegalFileNameChars(initialFileName);
    }
    String filePath = InternalConfig.getInstance().REMOTE_FUZZDIR + File.separator + initialFileName;

    filePath += FileTypeUtil.makeValidOrNoExtFile(fileType, r,
                                                  InternalConfig.getInstance().isAllowErroneousGeneration()
                                                      || InternalConfig.getInstance().isAllowNoExtFileTestaApi());
    return filePath;
  }

  /**
   * removing any semantic fuzzing randomness from the notification, as probably it won't affect
   * coverage, that is more based on the contents of the resources, the current focus of this
   * research.
   *
   * @param filename
   */
  private S3EventNotification.S3EventNotificationRecord constructS3EventNotificationRecord(
      String filename) {

    String eventTime = "2023-08-09T17:41:32.199+03:00"; //no need for selection
    String eventVersion = null;

    S3EventNotification.RequestParametersEntity requestParameters = generateRequestParametersEntity();

    S3EventNotification.ResponseElementsEntity responseElements =
        new S3EventNotification.ResponseElementsEntity("param1Value", "param2Value");

    String s3ObjectKey = filename;

    String bucketArn = "arn:aws:s3:::" + bucket.getName() + File.separator + s3ObjectKey;
    S3EventNotification.S3BucketEntity bucketEntity = new S3EventNotification.S3BucketEntity(
        bucket.getName(),
        new S3EventNotification.UserIdentityEntity(bucket.getOwner().getDisplayName()),
        bucketArn);

    S3EventNotification.S3ObjectEntity s3ObjectEntity = new S3EventNotification.S3ObjectEntity(
        s3ObjectKey,
        100L, // linking this with the actual object information is left for future work.
        "eTag",
        "",
        "");

    S3EventNotification.S3Entity s3 = new S3EventNotification.S3Entity("", bucketEntity,
                                                                       s3ObjectEntity,
                                                                       eventVersion);

    S3EventNotification.UserIdentityEntity userIdentity = new S3EventNotification.UserIdentityEntity(
        "some principle");

    return new S3EventNotification.S3EventNotificationRecord(
        REGION,
        EVENT_NAME,
        EVENT_SOURCE,
        eventTime,
        eventVersion,
        requestParameters,
        responseElements,
        s3,
        userIdentity
    );
  }


  private S3EventNotification.RequestParametersEntity generateRequestParametersEntity() {
    int subnet1 = 0;
    int subnet2 = 0;
    int subnet3 = 0;
    int subnet4 = 0;
    return new S3EventNotification.RequestParametersEntity(
        subnet1 + "." + subnet2 + "." + subnet3 + "." + subnet4);
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
    if (bucketTest != null)
      bucket = bucketTest;
  }
}
