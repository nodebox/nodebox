package nodebox.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ExportFormat {

    public static final ExportFormat PDF = new ExportFormat("PDF", "pdf");
    public static final ExportFormat PNG = new ExportFormat("PNG", "png");
    public static final ExportFormat SVG = new ExportFormat("SVG", "svg");
    public static final ExportFormat CSV = new ExportFormat("CSV", "csv");

    private static final Map<String, ExportFormat> FORMAT_MAP;

    static {
        FORMAT_MAP = new HashMap<>();
        FORMAT_MAP.put("PDF", PDF);
        FORMAT_MAP.put("PNG", PNG);
        FORMAT_MAP.put("SVG", SVG);
        FORMAT_MAP.put("CSV", CSV);
    }

    public static ExportFormat of(String name) {
        return FORMAT_MAP.get(name.toUpperCase(Locale.US));
    }

    private final String label;
    private final String extension;

    public ExportFormat(String label, String extension) {
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
        if (file.toLowerCase(Locale.US).endsWith("." + getExtension()))
            return file;
        return file + "." + getExtension();
    }

}
