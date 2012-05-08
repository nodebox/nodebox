package nodebox.movie;

import java.io.File;
import java.util.ArrayList;

public interface VideoFormat {
    public String getDisplayName();

    public String getExtension();

    public int getWidth();

    public int getHeight();

    public ArrayList<String> getArgumentList();

    public ArrayList<String> getArgumentList(Movie movie);

    public File ensureFileExtension(File file);

    public String ensureFileExtension(String file);
}
