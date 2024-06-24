package edu.umn.cs.spoton.front;

import com.amazonaws.services.s3.model.S3Object;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.s3.S3Generator;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class S3GeneratorNoExtensionTest {

  @BeforeClass
  public static void settingParameters() {
    InternalConfig.resetInstance();
    InternalConfig.getInstance().setLocalStackEndpoint("http://localhost:8010");
    S3Generator.bucketTest = S3Env.buckets.get(0);
    InternalConfig.getInstance().setAllowErroneousGeneration(true);
    InternalConfig.getInstance().setAllowNoExtFileTestApi(true);
    S3Env.getInstance().reset();
  }

  @After
  public void resetState() {
    InternalConfig.getInstance().setAllowNoExtFileTestApi(true);
  }


  @Fuzz
  public void generatorTest(@From(S3Generator.class) S3Object s3Object) {
//    System.out.println(s3Object.getKey());
    assert !InternalConfig.instance.isAllowNoExtFileTestaApi() && (!s3Object.getKey()
        .contains(".") || s3Object.getKey().endsWith(".gz") || s3Object.getKey().endsWith(".zip")) :
        "No extension can exist in the erroneous S3Object " + s3Object.getKey();
  }
}