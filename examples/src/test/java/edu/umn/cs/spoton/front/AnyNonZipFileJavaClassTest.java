package edu.umn.cs.spoton.front;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.FileGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class AnyNonZipFileJavaClassTest {

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    FileGenerator.fileTypeTest = FileType.JAVA_CLASS;
    FileGenerator.filenameTest =  InternalConfig.getInstance().LOCAL_FUZZDIR + "/file.class";
    FileManipulation.initializeDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }

  @After
  public void resetState() {
    FileManipulation.clearDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }

  @Fuzz
  public void generatorTest(@From(FileGenerator.class) File file) {
    System.out.println(file);
  }

}
