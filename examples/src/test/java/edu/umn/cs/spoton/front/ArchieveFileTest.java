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
public class ArchieveFileTest {

  static boolean choice = true;

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    FileGenerator.fileTypeTest = FileType.ARCHIVE;
    FileGenerator.filenameTest = InternalConfig.getInstance().LOCAL_FUZZDIR
        + "/file.zip";
    FileManipulation.initializeDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }

  @After
  public void choose() {
    if (choice)
      FileGenerator.filenameTest = InternalConfig.getInstance().LOCAL_FUZZDIR
          + "/file.zip";
    else
      FileGenerator.filenameTest = InternalConfig.getInstance().LOCAL_FUZZDIR
          + "/file.gz";

    choice = !choice;
  }

  @Fuzz
  public void generatorTestZip(@From(FileGenerator.class) File file) {
    System.out.println(file);
  }

  @Fuzz
  public void generatorTestGzip(@From(FileGenerator.class) File file) {
    System.out.println(file);
  }

}
