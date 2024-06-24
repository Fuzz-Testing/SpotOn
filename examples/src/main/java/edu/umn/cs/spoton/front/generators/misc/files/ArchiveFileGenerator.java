package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchiveFileGenerator extends Generator<File> {

  //  public static FileType fileTypeTest;
  public static String filenameTest;
  //  FileTypeUtil.FileType fileType;
  String filename = filenameTest != null ? filenameTest : null;

  public void setFilename(String filename) {
    this.filename = filename;
  }

  enum ArchiveType {
    ZIP {
      @Override
      public String toString() {
        return ".zip";
      }
    }, GZIP {
      @Override
      public String toString() {
        return ".gz";
      }
    }
  }

  private Boolean erroneousExtensionGeneration = false; // so far we are not using this flag, it is there for possible future work.

  public ArchiveFileGenerator() {
    super(File.class);
  }

  @Override
  public File generate(SourceOfRandomness r, GenerationStatus status) {
    assert erroneousExtensionGeneration
        != null : "erroneousGeneration flag must be set to use this generator. failing.";
    int numZipEntry = r.nextInt(0, 1);
    Set<File> files = new HashSet<>(); // ensures we wont find duplicate file in the zip, which is going to raise a ZipException.

    for (int i = 0; i <= numZipEntry; i++) {
      //choosing any non-zip file
      FileType compressedFileType = r.choose(
          Arrays.stream(FileType.values())
              .filter(
                  type -> type
                      != FileType.ARCHIVE)
              .toArray(FileType[]::new));
      String compressedFileName = makeName(r, status, compressedFileType);
      AnyNonZipFileGenerator anyNonZipFileGenerator = gen().make(AnyNonZipFileGenerator.class);
      anyNonZipFileGenerator.setFilename(compressedFileName);
      anyNonZipFileGenerator.setFileType(compressedFileType);
      File entryFile = anyNonZipFileGenerator.generate(r, status);
//      System.out.println("entry file created = " + entryFile.getAbsolutePath() + " existCheck = "
//                             + entryFile.exists());
      files.add(entryFile);
    }

//    String archiveType = makeValidOrErroneousFilename(FileTypeUtil.FileType.ARCHIVE, r,
//                                                      erroneousExtensionGeneration); //makeValidFileName(FileTypeUtil.FileType.ZIP, r);
    String archiveType = null;
    boolean typeIsErronous = false;
    if (filename.contains(".")) {
      archiveType = filename.substring(filename.lastIndexOf("."));
      typeIsErronous = !(archiveType.equals(ArchiveType.ZIP.toString())
          || archiveType.equals(ArchiveType.GZIP.toString()));
    } else
      typeIsErronous = true;

    if (typeIsErronous) { //erroneous case, filename will not have the extension part.
      archiveType = r.choose(ArchiveType.values()).toString();
      System.out.println("errornous file type select, randomly selected = " + archiveType);
      //checking the case where we are creating no extension for the zip file and that the entry within the zip has the same name, in which case, we need to
      // restore the zip file name to have .zip extension
      System.out.println("checking if " + filename + "is contained within \n" + files);
      File zipFullFile = new File(filename);
      if (files.stream().anyMatch(f -> f.equals(zipFullFile))) {
        System.out.println("archive file name matches name of one of its entries.");
        filename += archiveType;
      }
    }
//    filename += archiveType;

    if (archiveType.equals(ArchiveType.ZIP.toString()))
      compressZip(filename, files);
    else {
      assert archiveType.equals(
          ArchiveType.GZIP.toString()) :
          "archiveType=" + archiveType + " is an unexpected archive file type";
      assert files.size() > 0 : "there must exist at least a single file to archive";
      //archiving using gzip only needs a single file, we'll fix it to always be the first entry.
      compressGZip(filename, (File) files.toArray()[0]);
    }
    File createdFile = new File(filename);
    System.out.println("End of creation of archieve file name = " + createdFile.getAbsolutePath()
                           + " existCheck = " + createdFile.exists());
    return createdFile;
  }

  private String makeName(SourceOfRandomness r, GenerationStatus status, FileType fileType) {

    FreshOrConstantGenerator.setUseAnyStringGenerator(
        false); //we want to use alpha names for filenames
    String initialFileName = null;
    while (initialFileName == null || initialFileName.isEmpty() || FileTypeUtil.isRestrictedFileName(
        initialFileName)) {
      initialFileName = gen().make(FreshOrConstantGenerator.class).generate(r, status);
      initialFileName = FileTypeUtil.removeIllegalFileNameChars(initialFileName);
    }
    String filePath = InternalConfig.getInstance().LOCAL_FUZZDIR +  File.separator + initialFileName;

//    boolean generateNoExtension = false;
//    if (Config.allowErroneousGeneration)
//      generateNoExtension = r.nextFloat(0, 1) <= 0.05;

    filePath += FileTypeUtil.makeValidOrNoExtFile(fileType, r, InternalConfig.getInstance().isAllowErroneousGeneration());
    return filePath;
  }

  private void compressZip(String zipFullFileName, Set<File> files) {
//    //checking the case where we are creating no extension for the zip file and that the entry within the zip has the same name, in which case, we need to
//    // restore the zip file name to have .zip extension
//    System.out.println("checking if " + zipFullFileName + "is contained within \n" + files);
//    File zipFullFile = new File(zipFullFileName);
//
//    if (files.stream().anyMatch(f-> f.getAbsolutePath().equals(zipFullFile.getAbsolutePath()))) {
//      System.out.println("match occurred");
//      assert !zipFullFileName.endsWith(
//          ArchiveType.ZIP.toString()) : "not expecting to be creating a zip of a zip";
//      zipFullFileName += ".zip";
//    }
    System.out.println("current value for the zipFullFileName = " + zipFullFileName);
    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFullFileName))) {
      for (File fileToZip : files) {
        zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
        Files.copy(fileToZip.toPath(), zipOut);
        System.out.println("file added to zip = " + fileToZip.getAbsolutePath() + " checkExists = "
                               + fileToZip.exists());
        fileToZip.delete();
      }
    } catch (IOException e) {
      System.out.println("runtime exception has occured for " + e);
      throw new RuntimeException(e);
    }
  }

  private void compressGZip(String gzipFullFileName, File fileToArchive) {
    try {
      FileInputStream fis = new FileInputStream(fileToArchive);
      FileOutputStream fos = new FileOutputStream(gzipFullFileName);
      GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        gzipOS.write(buffer, 0, len);
      }
      //close resources
      gzipOS.close();
      fos.close();
      fis.close();
//      fileToArchive.delete();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}