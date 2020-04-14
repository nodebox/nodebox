package nodebox.movie;

import java.util.ArrayList;

public class MP4VideoFormat extends AbstractVideoFormat {
    public static final MP4VideoFormat LosslessFormat = new MP4VideoFormat("Lossless", 1);
    public static final MP4VideoFormat HighFormat = new MP4VideoFormat("High", 18);
    public static final MP4VideoFormat MediumFormat = new MP4VideoFormat("Medium", 40);
    public static final MP4VideoFormat LowFormat = new MP4VideoFormat("Low", 63);

    private int crf;

    private MP4VideoFormat(String displayName, int crf) {
        super(displayName, "mp4");
        this.crf = crf;
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        argumentList.add("-vcodec");
        argumentList.add("h264");
        argumentList.add("-crf");
        argumentList.add("" + crf);
        argumentList.add("-pix_fmt");
        argumentList.add("yuv420p");
        argumentList.add("-f");
        argumentList.add("mp4");
        return argumentList;
    }
}
