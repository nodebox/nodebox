package nodebox.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ImageFormat {

    public static final ImageFormat PDF = new ImageFormat("PDF", "pdf");
    public static final ImageFormat PNG = new ImageFormat("PNG", "png");

    private static final Map<String, ImageFormat> FORMAT_MAP;

    static {
        FORMAT_MAP = new HashMap<String, ImageFormat>();
        FORMAT_MAP.put("PDF", PDF);
        FORMAT_MAP.put("PNG", PNG);
    }

    public static ImageFormat of(String name) {
        return FORMAT_MAP.get(name.toUpperCase());
    }

    private final String label;
    private final String extension;

    public ImageFormat(String label, String extension) {
        this.label = label;
        this.extension = extension;
    }

    public String getLabel() {
        return label;
    }

    public String getExtension() {
        return extension;
    }

    public File ensureFileExtension(File file) {
        return new File(ensureFileExtension(file.getPath()));
    }

    public String ensureFileExtension(String file) {
        if (file.endsWith("." + getExtension()))
            return file;
        return file + "." + getExtension();
    }

}
