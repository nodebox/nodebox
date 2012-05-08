package nodebox.movie;

import java.util.ArrayList;

public class WebmVideoFormat extends AbstractVideoFormat {
    public static final WebmVideoFormat WebmFormat = new WebmVideoFormat();

    private WebmVideoFormat() {
        super("WebM (vp8)", "webm");
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        argumentList.add("-f");
        argumentList.add("webm");
        argumentList.add("-vcodec");
        argumentList.add("libvpx");
        argumentList.add("-crf");
        argumentList.add("22");
        return argumentList;
    }
}
