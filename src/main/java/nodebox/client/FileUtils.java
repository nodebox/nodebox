package nodebox.client;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.util.Locale;
import java.util.StringTokenizer;

public class FileUtils {
    /**
     * Gets the extension of a file.
     *
     * @param f the file
     * @return the extension of the file.
     */
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }

    /**
     * Gets the extension of a file.
     *
     * @param fileName the file name
     * @return the extension of the file.
     */
    public static String getExtension(String fileName) {
        String ext = null;
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1).toLowerCase(Locale.US);
        }
        return ext;
    }

    /**
     * Gets the name of a file without the extension.
     *
     * @param fileName the file name
     * @return the extension of the file.
     */
    public static String getBaseName(String fileName) {
        if (fileName == null) return null;
        int pos = fileName.lastIndexOf(".");
        // If there wasn't any '.' just return the string as is.
        if (pos == -1) return fileName;
        // Otherwise return the string, up to the dot.
        return fileName.substring(0, pos);
    }


    public static File showOpenDialog(Frame owner, String pathName, String extensions, String description) {
        return showFileDialog(owner, pathName, extensions, description, FileDialog.LOAD);
    }

    public static File showSaveDialog(Frame owner, String pathName, String extensions, String description) {
        return showFileDialog(owner, pathName, extensions, description, FileDialog.SAVE);
    }

    private static File showFileDialog(Frame owner, String pathName, String extensions, String description, int fileDialogType) {
        FileDialog fileDialog = new FileDialog(owner, pathName, fileDialogType);
        if (pathName == null || pathName.trim().length() == 0) {
            NodeBoxDocument document = NodeBoxDocument.getCurrentDocument();
            if (document != null) {
                File documentFile = document.getDocumentFile();
                if (documentFile != null) {
                    fileDialog.setDirectory(documentFile.getParentFile().getPath());
                }
            }
        } else {
            File f = new File(pathName);
            if (f.isDirectory()) {
                fileDialog.setDirectory(pathName);
            } else {
                if (f.getParentFile() != null) {
                    fileDialog.setDirectory(f.getParentFile().getPath());
                    fileDialog.setFile(f.getName());
                } else {
                    NodeBoxDocument document = NodeBoxDocument.getCurrentDocument();
                    if (document != null) {
                        File documentFile = document.getDocumentFile();
                        if (documentFile != null) {
                            fileDialog.setDirectory(documentFile.getParentFile().getPath());
                        }
                    }
                    fileDialog.setFile(pathName);
                }
            }
        }
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
        String description;

        public FileExtensionFilter(String extensions, String description) {
            this.extensions = parseExtensions(extensions);
            this.description = description;
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
            return description;
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

}
