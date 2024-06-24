package edu.umn.cs.spoton.front;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.FileGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class AnyNonZipFileXMLTest {

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    FileGenerator.fileTypeTest = FileType.XML;
    FileGenerator.filenameTest =  InternalConfig.getInstance().LOCAL_FUZZDIR + "/xmlFile.xml";
    FileManipulation.initializeDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }


  @Fuzz
  public void generatorTest(@From(FileGenerator.class) File file) {
    System.out.println(file);
  }

}
