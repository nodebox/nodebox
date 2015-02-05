package nodebox.movie;

import java.io.File;
import java.util.ArrayList;

public abstract class AbstractVideoFormat implements VideoFormat {
    private static final String SIZE_ARG_TEMPLATE = "%sx%s";
    private String displayName;
    private String extension;
    private int width, height;

    public AbstractVideoFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public AbstractVideoFormat(String displayName, String extension, int width, int height) {
        this(displayName, extension);
        this.width = width;
        this.height = height;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtension() {
        return extension;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    protected String getPresetLocation(String preset) {
        String format = String.format(Movie.FFMPEG_PRESET_TEMPLATE, preset);
        if (!new File(format).exists()) {
            format = nodebox.util.FileUtils.getApplicationFile(format).getAbsolutePath();
        }
        return format;
    }

    public ArrayList<String> getArgumentList() {
        return getArgumentList(null);
    }

    protected String getSizeArgument(int inputWidth, int inputHeight) {
        if (inputWidth > width || inputHeight > height) {
            float widthRatio = (float) inputWidth / width;
            float heightRatio = (float) inputHeight / height;
            float ratio = Math.max(widthRatio, heightRatio);
            return String.format(SIZE_ARG_TEMPLATE, roundEven((int) (inputWidth / ratio)), roundEven((int) (inputHeight / ratio)));
        }
        return null;
    }

    private int roundEven(int p) {
        return p + (p % 2);
    }

    public File ensureFileExtension(File file) {
        return new File(ensureFileExtension(file.getPath()));
    }

    public String ensureFileExtension(String file) {
        if (file.endsWith("." + getExtension()))
            return file;
        return file + "." + getExtension();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
