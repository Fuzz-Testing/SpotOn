package edu.umn.cs.spoton.front;


import com.amazonaws.services.s3.model.S3Object;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.s3.S3Generator;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class S3GeneratorDiffExtensionTest {

  @BeforeClass
  public static void settingParameters() {
    InternalConfig.resetInstance();
    InternalConfig.getInstance().localStackEndpoint = "http://localhost:8010";
    S3Env.getInstance().init(Collections.singletonList("my-bucket"));
    InternalConfig.getInstance().setAllowErroneousGeneration(false);
    InternalConfig.getInstance().setAllowDifferentExtFileTest(true);
    InternalConfig.getInstance().setAllowNoExtFileTestApi(false);
    S3Generator.bucketTest = S3Env.buckets.get(0);
    S3Env.getInstance().reset();
  }

  @After
  public void resetState() {
    InternalConfig.getInstance().setAllowDifferentExtFileTest(true);
  }


  @Fuzz
  public void generatorTest(@From(S3Generator.class) S3Object s3Object) {
    assert !InternalConfig.getInstance().isAllowDifferentExtFileTest() && s3Object.getKey()
        .contains(".") : "extension must exist in the erroneous S3Object";
  }
}