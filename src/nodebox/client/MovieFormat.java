package nodebox.client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MovieFormat {

    public static final MovieFormat MOV = new MovieFormat("MOV", "mov");
    public static final MovieFormat AVI = new MovieFormat("AVI", "avi");
    public static final MovieFormat MP4 = new MovieFormat("MP4", "mp4");

    private static final Map<String, MovieFormat> FORMAT_MAP;

    static {
        FORMAT_MAP = new HashMap<String, MovieFormat>();
        FORMAT_MAP.put("MOV", MOV);
        FORMAT_MAP.put("AVI", AVI);
        FORMAT_MAP.put("MP4", MP4);
    }

    public static MovieFormat of(String name) {
        return FORMAT_MAP.get(name.toUpperCase());
    }

    private final String label;
    private final String extension;

    public MovieFormat(String label, String extension) {
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
