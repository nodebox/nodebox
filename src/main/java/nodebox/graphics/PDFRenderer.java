package nodebox.graphics;

import nodebox.client.visualizer.Visualizer;
import nodebox.ui.ProgressDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDFRenderer {

    private static com.itextpdf.text.pdf.DefaultFontMapper fontMapper;

    static {
        final ProgressDialog pd = new ProgressDialog(null, "Preparing for PDF export");
        pd.setMessage("Loading Fonts");
        pd.setVisible(true);

        SwingWorker fontLoader = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
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
        com.itextpdf.text.Rectangle size = new com.itextpdf.text.Rectangle((float) bounds.getWidth(), (float) bounds.getHeight());
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
        Graphics2D graphics = contentByte.createGraphics((float) bounds.getWidth(), (float) bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        v.draw(graphics, objects);
        graphics.dispose();
        document.close();
    }

    public static void render(Grob g, File file) {
        // I'm using fully qualified class names here so as not to pollute the class' namespace.
        Rect bounds = g.getBounds();
        com.itextpdf.text.Rectangle size = new com.itextpdf.text.Rectangle((float) bounds.getWidth(), (float) bounds.getHeight());
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
        Graphics2D graphics = contentByte.createGraphics((float) bounds.getWidth(), (float) bounds.getHeight(), fontMapper);
        graphics.translate(-bounds.getX(), -bounds.getY());
        g.draw(graphics);
        graphics.dispose();
        document.close();
    }

}
