package nodebox.graphics;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import nodebox.client.visualizer.Visualizer;
import nodebox.ui.ProgressDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDFRenderer {

    private static DefaultFontMapper fontMapper;

    static {
        final ProgressDialog pd = new ProgressDialog(null, "Preparing for PDF export");
        pd.setMessage("Loading Fonts");
        pd.setVisible(true);

        SwingWorker fontLoader = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
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
                return null;
            }

            @Override
            protected void done() {
                pd.setVisible(false);
            }
        };
        fontLoader.execute();
    }

    public static void render(File file, Visualizer v, Iterable<?> objects) {
        // I'm using fully qualified class names here so as not to pollute the class' namespace.
        Rectangle2D bounds = v.getBounds(objects);
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
        Graphics2D graphics = new PdfGraphics2D(contentByte, (float) bounds.getWidth(), (float) bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        v.draw(graphics, objects);
        graphics.dispose();
        document.close();
    }

    public static void render(Grob g, File file) {
        // I'm using fully qualified class names here so as not to pollute the class' namespace.
        Rect bounds = g.getBounds();
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
        Graphics2D graphics = new PdfGraphics2D(contentByte, (float) bounds.getWidth(), (float) bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        g.draw(graphics);
        graphics.dispose();
        document.close();
    }

}
