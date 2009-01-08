package net.nodebox.client;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.util.StringTokenizer;

public class FileUtils {
    /**
     * Gets the extension of a file.
     */
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }

    /**
     * Gets the extension of a file.
     */
    public static String getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }


    public static File showOpenDialog(Frame owner, String pathName, String extensions, String description) {
        return showFileDialog(owner, pathName, extensions, description, FileDialog.LOAD);
    }

    public static File showSaveDialog(Frame owner, String pathName, String extensions, String description) {
        return showFileDialog(owner, pathName, extensions, description, FileDialog.SAVE);
    }

    private static File showFileDialog(Frame owner, String pathName, String extensions, String description, int fileDialogType) {
        FileDialog fileDialog = new FileDialog(owner, pathName, fileDialogType);
        fileDialog.setFilenameFilter(new FileExtensionFilter(extensions, description));
        fileDialog.setVisible(true);
        String chosenFile = fileDialog.getFile();
        String dir = fileDialog.getDirectory();
        if (chosenFile != null) {
            return new File(dir + chosenFile);
        } else {
            return null;
        }

    }

    public static String[] parseExtensions(String extensions) {
        StringTokenizer st = new StringTokenizer(extensions, ",");
        String[] ext = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ext[i++] = st.nextToken();
        }
        return ext;
    }

    public static class FileExtensionFilter extends FileFilter implements FilenameFilter {
        String[] extensions;
        String desc;

        public FileExtensionFilter(String extensions, String desc) {
            this.extensions = parseExtensions(extensions);
            this.desc = desc;
        }

        public boolean accept(File f) {
            if (f.isDirectory())
                return true;

            return accept(null, f.getName());
        }

        public boolean accept(File f, String s) {
            String extension = FileUtils.getExtension(s);
            if (extension != null) {
                for (int i = 0; i < extensions.length; i++) {
                    if (extensions[i].equals("*") || extensions[i].equalsIgnoreCase(extension)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getDescription() {
            return desc;
        }
    }

    public static String readFile(File file) {
        StringBuffer contents = new StringBuffer();
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + file, e);
        }
        return contents.toString();
    }

}
