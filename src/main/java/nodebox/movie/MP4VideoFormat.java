package nodebox.movie;

import java.util.ArrayList;

public class MP4VideoFormat extends AbstractVideoFormat {
    public static final MP4VideoFormat MP4Format = new MP4VideoFormat();

    private MP4VideoFormat() {
        super("MP4 Video", "mp4");
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        argumentList.add("-strict");
        argumentList.add("experimental");
        argumentList.add("-vcodec");
        argumentList.add("libx264");
        argumentList.add("-fpre");
        argumentList.add(getPresetLocation("slow"));
        argumentList.add("-f");
        argumentList.add("mp4");
        argumentList.add("-crf");
        argumentList.add("22");
        return argumentList;
    }
}
