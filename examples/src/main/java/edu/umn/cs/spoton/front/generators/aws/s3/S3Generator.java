package edu.umn.cs.spoton.front.generators.aws.s3;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.event.S3EventGenerator;
import edu.umn.cs.spoton.front.generators.aws.states.StatisticsManager;
import edu.umn.cs.spoton.front.generators.misc.files.FileGenerator;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

//Generate objects inside the bucket, by either updating existing objects or adding new objects.
public class S3Generator extends Generator<S3Object> {

  public static Bucket bucketTest; //parameter that is set when running unit tests.
  private Bucket bucket = bucketTest != null ? bucketTest : null;


  S3Event s3Event;

  public S3Event getS3Event() {
    return s3Event;
  }

  public S3Generator() {
    super(S3Object.class);
  }

  //  setting the bucket name needs to happen before generate method is invoked.
  @Override
  public S3Object generate(SourceOfRandomness r, GenerationStatus status) {
    Bucket bucket = r.choose(S3Env.buckets);

    ArrayList<S3Object> output = new ArrayList<>();

    ArrayList<File> localFiles = new ArrayList<>();
    S3EventGenerator s3EventGenerator = gen().make(S3EventGenerator.class);
    s3EventGenerator.setBucket(bucket);
    s3Event = s3EventGenerator.generate(r, status);

    String fileName =
        InternalConfig.getInstance().buildDir + File.separator + s3Event.getRecords().get(0).getS3()
            .getObject().getKey();
    FileGenerator fileGenerator = gen().make(FileGenerator.class);
    fileGenerator.setFileName(fileName);
    Instant beforeDate = Instant.now();
    File generatedFile = fileGenerator.generate(r, status);
    Instant afterDate = Instant.now();
    localFiles.add(generatedFile);
    long diff = Duration.between(beforeDate, afterDate).toMillis();
    StatisticsManager.addS3States(localFiles.get(0), diff); //since we have a single file.

    S3Env.getInstance().populateBatchToCloud(bucket, localFiles);
    localFiles.forEach(
        f -> output.add(
            S3Env.s3Client.getObject(bucket.getName(),
                                     InternalConfig.getInstance().REMOTE_FUZZDIR + File.separator
                                         + f.getName())));

    assert output.size() == 1 : "this is a single S3Object generator, but assumption violated";
    return output.get(0);
  }

}
