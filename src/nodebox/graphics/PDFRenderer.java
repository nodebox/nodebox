package nodebox.graphics;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDFRenderer {

    private static com.lowagie.text.pdf.DefaultFontMapper fontMapper;

    static {
        fontMapper = new com.lowagie.text.pdf.DefaultFontMapper();
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            // TODO: Windows is not installed under C:\Windows all the time.
            fontMapper.insertDirectory("C:\\windows\\fonts");
        } else if (osName.startsWith("Mac OS X")) {
            fontMapper.insertDirectory("/Library/Fonts");
            String userHome = System.getProperty("user.home");
            fontMapper.insertDirectory(userHome + "/Fonts");
        } else {
            // Where are the fonts in a UNIX install?
        }
    }

    public static void render(Grob g, File file) {
        Rect bounds = g.getBounds();
        // I'm using fully qualified class names here so as not to polute the class' namespace.
        com.lowagie.text.Rectangle size = new com.lowagie.text.Rectangle(bounds.getWidth(), bounds.getHeight());
        com.lowagie.text.Document document = new com.lowagie.text.Document(size);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The file " + file + "could not be created", e);
        }
        com.lowagie.text.pdf.PdfWriter writer;
        try {
            writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, fos);
        } catch (com.lowagie.text.DocumentException e) {
            throw new RuntimeException("An error occurred while creating a PdfWriter object.", e);
        }
        document.open();
        com.lowagie.text.pdf.PdfContentByte contentByte = writer.getDirectContent();
        Graphics2D graphics = contentByte.createGraphics(bounds.getWidth(), bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        g.draw(graphics);
        graphics.dispose();
        document.close();
    }

}
