package edu.umn.cs.spoton.front.env;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.ServiceBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

//holds and controls the S3Environment.
public class S3Env extends BasicEnv {

  static S3Env instance = new S3Env();

//  static String DEFAULT_BUCKET_NAME = "my-bucket";

  public static List<Bucket> buckets;
  public static AmazonS3 s3Client;

  public static S3Env getInstance() {
    return instance;
  }

  @Override
  public void init(Object bucketNames) {
    super.init(null);
    s3Client = ServiceBuilder.prepareS3();
    buckets = s3Client.listBuckets();
    deleteBuckets(); //clearing old data, if exists
//    if (((ArrayList) bucketNames).isEmpty())
//      ((ArrayList<String>) bucketNames).add(DEFAULT_BUCKET_NAME);
    createBuckets((List<String>) bucketNames);
    do {buckets = s3Client.listBuckets();} while (buckets.isEmpty());
  }

  @Override
  public void reset() {
    super.reset();
    clearBuckets();
  }

  //deleting all buckets, this happens once when the system is starting fresh.
  public void deleteBuckets() {
    clearBuckets();
    for (Bucket bucket : buckets) {
      s3Client.deleteBucket(bucket.getName());
    }
  }

  public void createBuckets(List<String> bucketNames) {
    for (String bucketName : bucketNames)
      s3Client.createBucket(bucketName);
  }


  private void clearBuckets() {
    for (Bucket bucket : buckets) {
      do {
        List<S3ObjectSummary> s3Summaries = s3Client.listObjects(bucket.getName())
            .getObjectSummaries();

        String[] s3Keys = s3Summaries.stream().map(s3Obj -> s3Obj.getKey()).toArray(String[]::new);

        if (s3Keys.length > 0) {
          DeleteObjectsRequest dor = new DeleteObjectsRequest(bucket.getName())
              .withKeys(s3Keys).withQuiet(false);
          s3Client.deleteObjects(dor);
        }
      } while (s3Client.listObjects(bucket.getName()).getObjectSummaries().size() != 0);
      if (InternalConfig.getInstance().isDebugModeOn())
        assert s3Client.listObjects(bucket.getName()).getObjectSummaries().size() == 0 :
            "bucket not empty (" + bucket.getName() + ")";
    }
  }


  public void populateBatchToCloud(Bucket bucket, ArrayList<File> localFiles) {
//    List<String> fileNames = localFiles.stream().map(f -> f.getName()).collect(Collectors.toList());

    for (File file : localFiles)
      assert file.exists() : "file = " + file.getAbsolutePath() + " does not exist";
    TransferManager transfer = TransferManagerBuilder.standard().withS3Client(s3Client).build();
    if (transfer == null || s3Client == null || bucket == null) {
      assert false : "transfer object cannot be null";
    }
    MultipleFileUpload upload = transfer.uploadFileList(bucket.getName(),
                                                        InternalConfig.getInstance().REMOTE_FUZZDIR,
                                                        new File(
                                                            InternalConfig.getInstance().LOCAL_FUZZDIR),
                                                        localFiles);
    try {
      upload.waitForCompletion();
      blockMainThreadUntilTransfer(upload);
      if (InternalConfig.getInstance().isDebugModeOn())
        System.out.println("upload is done.");
    } catch (InterruptedException e) {
      assert false : "upload of files for fuzzing failed for bucket = " + bucket;
    } finally {
      transfer.shutdownNow();
      s3Client = ServiceBuilder.prepareS3(); //reopen client
    }
  }

  private void blockMainThreadUntilTransfer(MultipleFileUpload upload)
      throws InterruptedException {
    while (!upload.isDone()) {
      Thread.sleep(200);
    }
  }
}
