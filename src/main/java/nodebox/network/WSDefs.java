package nodebox.network;

/**
 * Created by Gear on 12/5/2016.
 */
public class WSDefs {

    public static final String TYPE =      "type";
    public static final String ID =        "id";
    public static final String DOCID =     "docid";
    public static final String MESSAGE =   "msg";

    // Message Types
    public static final String DATA =      "dta";
    public static final String COMMAND =   "cmd";
    public static final String REQUEST =   "req";
    public static final String RESPONSE =  "rsp";



    public static class CMDS {
        public static final String PLAY =           "play";
        public static final String STOP =           "stop";
        public static final String REWIND =         "rewind";
        public static final String SETFRAME =       "setframe";
        public static final String RELOAD =         "reload";
        public static final String LOAD =           "load";
        public static final String EXPORTRANGE =    "exportrange";

    }


    public static class TAGS {
        public static final String FRAMENUMBER =    "frameNumber";
        public static final String FILENAME =       "fileName";
        public static final String EXPORTPATH =     "exportPath";
        public static final String STARTFRAME =     "start";
        public static final String ENDFRAME =       "end";
        public static final String EXPORTFORMAT =   "format";

        public static final String POINT =          "Point";
        public static final String CLASS =          "class";
        public static final String X =              "x";
        public static final String Y =              "y";
        public static final String Z =              "z";
        public static final String RECT =           "Rect";
        public static final String HEIGHT =         "height";
        public static final String WIDTH =          "width";
        public static final String POSITION =       "position";
        public static final String COLOR =          "Color";
        public static final String R =              "r";
        public static final String G =              "g";
        public static final String B =              "b";
        public static final String A =              "a";
        public static final String PATH =           "path";
        public static final String PATHS =          "paths";
        public static final String POINTCOUNT =     "pointCount";
        public static final String POINTS =         "points";
        public static final String BOUNDS =         "bounds";
        public static final String FILLCOLOR =      "fillColor";
        public static final String STROKECOLOR =    "strokeColor";
        public static final String STROKEWIDTH =    "strokeWidth";
        public static final String STROKECLOSED =   "strokeClosed";
        public static final String GEOMETRY =       "geometry";
        public static final String FRAME =          "frame";
        public static final String GETFRAME =       "getframe";
        public static final String GETDOCS =        "getdocs";
        public static final String GETDOC =         "getdoc";
        public static final String DOCUMENTS =      "docs";
    }
}
