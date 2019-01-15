package ai.cs;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Converter {

  public enum ImageFormat
  {
    BMP,
    JPEG,
    GIF,
    TIFF,
    PNG,
    PDF,
    UNKNOWN
  }

  private ImageFormat detectDocumentFormat(File file) throws IOException {
    byte[] buf4 = new byte[4];
    try (FileInputStream fin = new FileInputStream(file))
    {
      int count = fin.read(buf4);
      if (count != 4) {
        return ImageFormat.UNKNOWN; // probably not an image file
      }
    }

    byte[] buf2 = new byte[] { buf4[0], buf4[1] };
    byte[] buf3 = new byte[] { buf4[0], buf4[1], buf4[2] };

    byte[] bmp = "BM".getBytes(); // BMP
    byte[] gif = "GIF".getBytes(); // GIF
    byte[] png = new byte[] { (byte)137, 80, 78, 71 }; // PNG
    byte[] tiff = new byte[] { 73, 73, 0, 42 }; // TIFF
    byte[] tiff2 = new byte[] { 77, 77, 0, 42 }; // TIFF
    byte[] jpeg = new byte[] { (byte)255, (byte)216, (byte)255 }; // JPEG
    byte[] pdf = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // PDF

    if (Arrays.equals(buf4, pdf)) {
      return ImageFormat.PDF;
    }
    if (Arrays.equals(buf2, bmp)) {
      return ImageFormat.BMP;
    }
    if (Arrays.equals(buf3, gif)) {
      return ImageFormat.GIF;
    }
    if (Arrays.equals(buf4, png)) {
      return ImageFormat.PNG;
    }
    if (Arrays.equals(buf4, tiff) || Arrays.equals(buf4, tiff2)) {
      return ImageFormat.TIFF;
    }
    if (Arrays.equals(buf3, jpeg)) {
      return ImageFormat.JPEG;
    }

    return ImageFormat.UNKNOWN;
  }

  private void createPdfFromImage(String inputFile, String outputFile) throws IOException {
    try (PDDocument document = new PDDocument();
        InputStream in = new FileInputStream(inputFile))
    {
      BufferedImage bimg = ImageIO.read(in);
      float width = bimg.getWidth();
      float height = bimg.getHeight();
      PDPage page = new PDPage(new PDRectangle(width, height));
      document.addPage(page);

      PDImageXObject pdImage = LosslessFactory.createFromImage(document, bimg);

      try (PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true))
      {
        float scale = 1f;
        contentStream.drawImage(pdImage, 20, 20, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
      }
      document.save(outputFile);
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: Convert <directory>");
      return;
    }

    Converter converter = new Converter();

    try {
      File directory = new File(args[0]);
      File[] files = directory.listFiles();
      if (files == null) {
        System.err.println("Unable to find directory " + directory.getAbsolutePath());
        return;
      }
      for (File file : files) {
        ImageFormat format = converter.detectDocumentFormat(file);
        System.out.print(String.format("Format of %s format is %s", file.getName(), format));
        switch (format) {
          case UNKNOWN:
            System.out.println(" -- skipping unknown file format");
            break;
          case PDF:
            System.out.println(" -- PDF file already exists");
            break;
          default:
            String outfile = String.format("%s.pdf", file.getName());
            try {
              converter.createPdfFromImage(file.getAbsolutePath(), outfile);
              System.out.println(" -- converted to " + outfile);
            }
            catch (IOException e) {
              System.err.println(e.getMessage());
            }
            break;
        }
      }
    }
    catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
