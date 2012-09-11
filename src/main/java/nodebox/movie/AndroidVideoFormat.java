package nodebox.movie;

import java.util.ArrayList;

public class AndroidVideoFormat extends AbstractVideoFormat {
    public static final int DEFAULT_WIDTH = 480;
    public static final int DEFAULT_HEIGHT = 320;
    public static final int NEXUS_WIDTH = 800;
    public static final int NEXUS_HEIGHT = 480;
    public static final int DROID_WIDTH = 854;
    public static final int DROID_HEIGHT = 480;

    public static final AndroidVideoFormat DefaultFormat = new AndroidVideoFormat(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    public static final AndroidVideoFormat NexusFormat = new AndroidVideoFormat(NEXUS_WIDTH, NEXUS_HEIGHT);
    public static final AndroidVideoFormat DroidFormat = new AndroidVideoFormat(DROID_WIDTH, DROID_HEIGHT);

    private AndroidVideoFormat(int width, int height) {
        super(String.format("Android %sx%s", width, height), "mp4", width, height);
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        argumentList.add("-strict");
        argumentList.add("experimental");
        if (movie != null) {
            String sizeArg = getSizeArgument(movie.getWidth(), movie.getHeight());
            if (sizeArg != null) {
                argumentList.add("-s");
                argumentList.add(sizeArg);
            }
        }
        argumentList.add("-vcodec");
        argumentList.add("libx264");
        argumentList.add("-fpre");
        argumentList.add(getPresetLocation("slow"));
        argumentList.add("-fpre");
        argumentList.add(getPresetLocation("ipod640"));
        argumentList.add("-crf");
        argumentList.add("22");
        argumentList.add("-f");
        argumentList.add("mp4");
        argumentList.add("-threads");
        argumentList.add("0");
        return argumentList;
    }
}
