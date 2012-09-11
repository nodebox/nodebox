package nodebox.movie;

import java.util.ArrayList;

public class PSPVideoFormat extends AbstractVideoFormat {
    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 240;

    public static final PSPVideoFormat PSPFormat = new PSPVideoFormat();

    private PSPVideoFormat() {
        super("PSP", "mp4", DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        argumentList.add("-s");
        argumentList.add(String.format("%sx%s", getWidth(), getHeight()));
        argumentList.add("-b");
        argumentList.add("512000");
        argumentList.add("-f");
        argumentList.add("psp");
        argumentList.add("-r");
        argumentList.add("29.97");
        return argumentList;
    }
}
