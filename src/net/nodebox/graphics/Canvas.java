/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nodebox.graphics;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import net.nodebox.client.PlatformUtils;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Canvas extends Group {

    public static final double DEFAULT_WIDTH = 1000;
    public static final double DEFAULT_HEIGHT = 1000;

    private Color background = new Color(1, 1, 1);
    private double width, height;

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(double width, double height) {
        setSize(width, height);
    }

    public Canvas(Canvas other) {
        super(other);
        this.width = other.width;
        this.height = other.height;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Canvas clone() {
        return new Canvas(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Canvas)) return false;
        Canvas other = (Canvas) obj;
        return width == other.width
                && height == other.height
                && background.equals(other.background)
                && super.equals(other);
    }

    //// Drawing ////

    @Override
    public void draw(Graphics2D g) {
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double left = -halfWidth;
        double right = halfWidth;
        double top = -halfHeight;
        double bottom = halfHeight;
        g.setColor(background.getAwtColor());
        g.fillRect((int) left, (int) top, (int) width, (int) height);
        Rectangle clip = g.getClipBounds();
        int clipwidth = clip != null && width > clip.width ? clip.width : (int) height;
        int clipheight = clip != null && height > clip.height ? clip.height : (int) width;
        g.setClip(clip != null ? clip.x : 0, clip != null ? clip.y : 0, clipwidth, clipheight);
        super.draw(g);
    }

    public void save(File file) {
        if (file.getName().endsWith(".pdf")) {
            // I'm using fully qualified class names here so as not to polute the class' namespace.
            com.lowagie.text.Rectangle size = new com.lowagie.text.Rectangle((float) width, (float) height);
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            document = new Document(size);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("The file " + file + "could not be created", e);
            }
            com.lowagie.text.pdf.PdfWriter writer = null;
            try {
                writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, fos);
            } catch (DocumentException e) {
                throw new RuntimeException("An error occurred while creating a PdfWriter object.", e);
            }
            document.open();
            com.lowagie.text.pdf.DefaultFontMapper fontMapper = new com.lowagie.text.pdf.DefaultFontMapper();
            if (PlatformUtils.onWindows()) {
                fontMapper.insertDirectory("C:\\windows\\fonts");
            } else if (PlatformUtils.onMac()) {
                fontMapper.insertDirectory("/Library/Fonts");
            } else {
                // Where are the fonts in a UNIX install?
            }
            com.lowagie.text.pdf.PdfContentByte contentByte = writer.getDirectContent();
            Graphics2D graphics = contentByte.createGraphics(size.getWidth(), size.getHeight(), fontMapper);
            draw(graphics);
            graphics.dispose();
            document.close();
        } else {
            throw new UnsupportedOperationException("Unsupported file extension " + file);
        }
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + width + ", " + height + ">";
    }
}
