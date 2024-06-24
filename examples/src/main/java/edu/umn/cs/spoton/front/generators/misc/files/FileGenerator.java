package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileGenerator extends Generator<File> {


  // begin of dummy fields used for testing
  public static FileType fileTypeTest;
  public static String filenameTest;
  // end of dummy testing fields.

  FileType fileType = fileTypeTest != null ? fileTypeTest : null;
  String filename = filenameTest != null ? filenameTest : null;


  public FileGenerator() {
    super(File.class);
  }

  @Override
  public File generate(SourceOfRandomness r, GenerationStatus status) {
    assert filename != null : "filename cannot be null at this point";
//    assert erroneousExtensionGeneration
//        != null : "SpotOn:erroneousGeneration flag must be set to use this generator. failing.";

    File localFile = null;
    boolean erroneousNoExtension = false;
    boolean erroneousMismatchedContent = false;
    if (fileType == null) {
      erroneousNoExtension = true;
      fileType = r.choose(FileType.values());
      if (InternalConfig.getInstance().isDebugModeOn())
        System.out.println("error file generation of type = " + fileType);
    } else if (InternalConfig.getInstance().isAllowDifferentExtFileTest() || (
        InternalConfig.getInstance().isAllowErroneousGeneration()
            && r.nextFloat(0, 1) < InternalConfig.getInstance().PROBABILITY_OF_ERRONEOUS_FILE)) {
      erroneousMismatchedContent = true;
      fileType = r.choose(FileType.values()); //we generate a possibly different file
    }
    switch (fileType) {
      case XML:
      case IMGE:
      case JAVA_CLASS:
      case JAVA_SCRIPT:
      case TEXT:
      case CSV:
        AnyNonZipFileGenerator anyNonZipFileGenerator = gen().make(AnyNonZipFileGenerator.class);
        anyNonZipFileGenerator.setFilename(filename);
        anyNonZipFileGenerator.setFileType(fileType);
        localFile = anyNonZipFileGenerator.generate(r, status);
        break;
      case ARCHIVE:
        ArchiveFileGenerator archiveFileGenerator = gen().make(ArchiveFileGenerator.class);
        archiveFileGenerator.setFilename(filename);
        localFile = archiveFileGenerator.generate(r, status);
        break;
    }
    assert localFile != null : "generated file should not be null. failing.";

    if (erroneousNoExtension) {
      InternalConfig.getInstance()
          .setAllowNoExtFileTestApi(false); //turing this off to pass the erroneous unit test in this case
      if (localFile.getName().contains(".")) { //case if archive
        System.out.println(
            "no file extension containing extension, now renaming to no extension, for "
                + localFile.getAbsolutePath());
        // trimming the extension from the generated filename
        String generatedFileStrNoExt = localFile.getAbsolutePath()
            .substring(0, localFile.getAbsolutePath().lastIndexOf("."));
        File generatedFileNoExt = new File(generatedFileStrNoExt);
        System.out.println("renaming failed for file" + localFile.getAbsolutePath());
        System.out.println("to file" + generatedFileNoExt.getAbsolutePath());
        try {
          Files.copy(localFile.toPath(), generatedFileNoExt.toPath(),
                     StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else if (erroneousMismatchedContent) { //file has the same extension as the event, but the contents are possibly not correct for that type
      InternalConfig.getInstance().setAllowDifferentExtFileTest(
          false); //turing this off to pass the erroneous unit test in this case
    }
    return localFile;
  }

  public void setFileName(String filename) {
    this.filename = filename;
    String noPathFileName = filename.substring(filename.lastIndexOf(File.separator));
    if (noPathFileName.contains(".")) {
      String generatedFileType = noPathFileName.substring(noPathFileName.lastIndexOf("."));
      if (InternalConfig.getInstance().isDebugModeOn())
        System.out.println(" filename = " + filename);
      fileType = FileTypeUtil.fileTypeExtensionToEnum(generatedFileType);
    } else {
      assert InternalConfig.getInstance()
          .isAllowErroneousGeneration() : "unexpected erroneous filename without proper configuration.";
      fileType = null;
    }
  }
}
