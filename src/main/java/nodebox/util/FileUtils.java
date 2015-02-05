package nodebox.util;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class FileUtils {

    public static final String SEPARATOR = "/";

    /**
     * Returns the file name without its path and extension.
     * <p/>
     * If the file has no extension, the file name is returned as is.
     *
     * @param f the file
     * @return the file name without extension
     */
    public static String stripExtension(File f) {
        return stripExtension(f.getName());
    }

    /**
     * Returns the file name without its path and extension.
     * <p/>
     * If the file has no extension, the file name is returned as is.
     *
     * @param fileName the file name
     * @return the file name without extension
     */
    public static String stripExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i == -1) return fileName;
        return fileName.substring(0, i);
    }

    /**
     * Gets the extension of a file in lowercase.
     *
     * @param f the file
     * @return the extension of the file.
     */
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }

    /**
     * Gets the extension of a file in lowercase.
     *
     * @param fileName the file name
     * @return the extension of the file.
     */
    public static String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i == -1) return "";
        return fileName.substring(i + 1).toLowerCase(Locale.US);
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
            return f.isDirectory() || accept(null, f.getName());
        }

        public boolean accept(File f, String s) {
            String extension = FileUtils.getExtension(s);
            if (extension != null) {
                for (String extension1 : extensions) {
                    if (extension1.equals("*") || extension1.equalsIgnoreCase(extension)) {
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

    public static void writeFile(File file, String s) {
        try {
            Writer out = new BufferedWriter(new FileWriter(file));
            out.write(s);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not write file " + file, e);
        }
    }

    public static File createTemporaryDirectory(String prefix) {
        File tempDir = null;
        try {
            tempDir = File.createTempFile(prefix, "");
        } catch (IOException e) {
            throw new RuntimeException("Could not create temporary file " + prefix);
        }
        boolean success = tempDir.delete();
        if (!success) throw new RuntimeException("Could not delete temporary file " + tempDir);
        success = tempDir.mkdir();
        if (!success) throw new RuntimeException("Could not create temporary directory " + tempDir);
        return tempDir;
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
        return (directory.delete());
    }


    public static String getFullPath(File f) {
        try {
            return f.getCanonicalPath().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Could not get canonical path of file " + f, e);
        }
    }

    /**
     * Returns the path of one File relative to another.
     * <p/>
     * From http://stackoverflow.com/questions/204784
     *
     * @param target the target directory
     * @param base   the base directory
     * @return target's path relative to the base directory
     */
    public static String getRelativePath(File target, File base) {
        String[] baseComponents;
        String[] targetComponents;
        baseComponents = getFullPath(base).split(Pattern.quote(SEPARATOR));
        targetComponents = getFullPath(target).split(Pattern.quote(SEPARATOR));

        // skip common components
        int index = 0;
        for (; index < targetComponents.length && index < baseComponents.length; ++index) {
            if (!targetComponents[index].equals(baseComponents[index]))
                break;
        }

        StringBuilder result = new StringBuilder();
        if (index != baseComponents.length) {
            // backtrack to base directory
            for (int i = index; i < baseComponents.length; ++i)
                result.append("..").append(SEPARATOR);
        }
        for (; index < targetComponents.length; ++index)
            result.append(targetComponents[index]).append(SEPARATOR);
        if (!target.getPath().endsWith("/") && !target.getPath().endsWith("\\")) {
            // remove final path separator
            result.delete(result.length() - SEPARATOR.length(), result.length());
        }
        return result.toString();
    }

    /**
     * Returns the path of one File relative to another.
     * Returns the absolute path of the target file when there is no base file.
     * <p/>
     *
     * @param target the target directory
     * @param base   the base directory
     * @return target's path relative to the base directory
     */
    public static String getRelativeLink(File target, File base) {
        if (base == null) {
            return getFullPath(target);
        } else {
            return getRelativePath(target, base);
        }
    }

    public static File getApplicationFile(String path) {
        File f = new File(path);
        if (f.exists()) return f;
        final URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
        final File jarFile;
        try {
            jarFile = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return new File(jarFile.getParentFile(), path);
    }
}
