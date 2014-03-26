package nodebox.graphics;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDFRenderer {

    private static boolean initialized = false;
    private static DefaultFontMapper fontMapper;

    /**
     * Initialize the PDF renderer.
     * <p/>
     * This method loads all system fonts, and thus can take a while.
     * <p/>
     * You don't have to call this method explicitly: it is called before every render operation. However,
     * making it available here means you can show a dialog to the user that the export will take a while.
     * <p/>
     * This method can be called multiple times.
     */
    public static void initialize() {
        if (initialized) return;
        fontMapper = new DefaultFontMapper();
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
        initialized = true;
    }

    public static void render(Drawable drawable, Rect bounds, File file) {
        render(drawable, new Rectangle2D.Double(0, 0, bounds.width, bounds.height), file);
    }

    public static void render(Drawable drawable, Rectangle2D bounds, File file) {
        initialize();
        Rectangle size = new Rectangle((float) bounds.getWidth(), (float) bounds.getHeight());
        Document document = new Document(size);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The file " + file + "could not be created", e);
        }
        PdfWriter writer;
        try {
            writer = PdfWriter.getInstance(document, fos);
        } catch (DocumentException e) {
            throw new RuntimeException("An error occurred while creating a PdfWriter object.", e);
        }
        document.open();
        PdfContentByte contentByte = writer.getDirectContent();
        Graphics2D g = new PdfGraphics2D(contentByte, (float) bounds.getWidth(), (float) bounds.getHeight(), fontMapper);
        g.translate(-bounds.getX(), -bounds.getY());
        drawable.draw(g);
        g.dispose();
        document.close();
    }

}
