package nodebox.graphics;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDFRenderer {

    private static com.itextpdf.text.pdf.DefaultFontMapper fontMapper;

    static {
        fontMapper = new com.itextpdf.text.pdf.DefaultFontMapper();
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
        render(g, g.getBounds(), null, file);
    }

    public static void render(Grob g, Rect bounds, Color background, File file) {
        // I'm using fully qualified class names here so as not to polute the class' namespace.
        com.itextpdf.text.Rectangle size = new com.itextpdf.text.Rectangle(bounds.getWidth(), bounds.getHeight());
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(size);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The file " + file + "could not be created", e);
        }
        com.itextpdf.text.pdf.PdfWriter writer;
        try {
            writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, fos);
        } catch (com.itextpdf.text.DocumentException e) {
            throw new RuntimeException("An error occurred while creating a PdfWriter object.", e);
        }
        document.open();
        com.itextpdf.text.pdf.PdfContentByte contentByte = writer.getDirectContent();
        Graphics2D graphics = contentByte.createGraphics(bounds.getWidth(), bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        if (background != null) {
            graphics.setColor(background.getAwtColor());
            graphics.fillRect((int) bounds.getX(), (int) bounds.getY(), (int) bounds.getWidth(), (int) bounds.getHeight());
        }
        g.draw(graphics);
        graphics.dispose();
        document.close();
    }

}
