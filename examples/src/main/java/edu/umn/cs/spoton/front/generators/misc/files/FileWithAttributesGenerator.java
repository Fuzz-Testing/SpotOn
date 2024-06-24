package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.structured.date.CalendarGenerator;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.GregorianCalendar;

public class FileWithAttributesGenerator extends Generator<File> {

  public FileWithAttributesGenerator() {
    super(File.class);
  }

  @Override
  public File generate(SourceOfRandomness r, GenerationStatus status) {


    String fileName = generateFileName(r, status);

    FileGenerator fileGenerator = gen().make(FileGenerator.class);
    fileGenerator.setFileName(fileName);
    File generatedFile = fileGenerator.generate(r, status);

    boolean attrHasChanged = false;
    while (!attrHasChanged) { //repeat until we have generated successfully changed the file's attribute.
      GregorianCalendar randomCreationCalendar = gen().make(CalendarGenerator.class)
          .generate(r, status);
      GregorianCalendar lastAccessCalendar = gen().make(CalendarGenerator.class)
          .generate(r, status);
      GregorianCalendar lastModificationCalendar = gen().make(CalendarGenerator.class)
          .generate(r, status);
      // Convert long timestamps to FileTime
      FileTime creationTime = FileTime.fromMillis(randomCreationCalendar.getTimeInMillis());
      FileTime lastAccessTime = FileTime.fromMillis(lastAccessCalendar.getTimeInMillis());
      FileTime lastModificationTime = FileTime.fromMillis(
          lastModificationCalendar.getTimeInMillis());
      BasicFileAttributeView fileAttributeView = Files.getFileAttributeView(generatedFile.toPath(),
                                                                            BasicFileAttributeView.class);
      try {
        fileAttributeView.setTimes(lastModificationTime, lastAccessTime, creationTime);
        attrHasChanged = true;
      } catch (IOException e) {
        System.out.println("runtime exception has been thrown");
        throw new RuntimeException(e);
      }
    }
    return generatedFile;
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
    String filePath = InternalConfig.getInstance().LOCAL_FUZZDIR + File.separator + initialFileName;

    filePath += FileTypeUtil.makeValidOrNoExtFile(fileType, r,
                                                  InternalConfig.getInstance().isAllowErroneousGeneration()
                                                      || InternalConfig.getInstance().isAllowNoExtFileTestaApi());
    return filePath;
  }
}

