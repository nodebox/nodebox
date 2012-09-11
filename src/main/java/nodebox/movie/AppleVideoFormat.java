package nodebox.movie;

import java.util.ArrayList;

public class AppleVideoFormat extends AbstractVideoFormat {
    public static final int DEFAULT_WIDTH = 480;
    public static final int DEFAULT_HEIGHT = 320;
    public static final int IPAD_WIDTH = 1024;
    public static final int IPAD_HEIGHT = 768;

    public static final AppleVideoFormat DefaultFormat = new AppleVideoFormat("iPhone/iPod", DEFAULT_WIDTH, DEFAULT_HEIGHT);
    public static final AppleVideoFormat IpadFormat = new AppleVideoFormat("iPad", IPAD_WIDTH, IPAD_HEIGHT);

    private AppleVideoFormat(String displayName, int width, int height) {
        super(displayName, "mp4", width, height);
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
        argumentList.add("-b");
        argumentList.add("1200k");
        argumentList.add("-f");
        argumentList.add("mp4");
        argumentList.add("-threads");
        argumentList.add("0");
        return argumentList;
    }
}
