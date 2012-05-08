package nodebox.localhistory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Properties;

public class Repository {

    private LocalHistoryManager manager;
    private String projectName;
    private File directory;
    private File projectDirectory;

    public Repository(LocalHistoryManager manager, String projectName) {
        this.manager = manager;
        this.projectName = projectName;
        this.directory = new File(manager.getLocalHistoryDirectory(), projectName);
        if (!this.directory.exists())
            throw new AssertionError("Repository directory " + this.directory + " does not exist.");
        // Parse the configuration to set the project directory.
        parseConfiguration();
    }

    /**
     * Parse the configuration file, stored under the config directory.
     * <p/>
     * The configuration file holds the repository format version and the project path.
     */
    private void parseConfiguration() {
        File configPath = new File(directory, "config");
        if (!configPath.exists())
            throw new AssertionError("Repository " + projectName + " does not have a config file.");
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read config file for repository " + projectName, e);
        }
        String formatVersion = properties.getProperty("repositoryformatversion");
        if (!formatVersion.equals("0"))
            throw new AssertionError("Project " + projectName + ": unsupported repository format version.");
        projectDirectory = new File(properties.getProperty("projectpath"));
        if (!projectDirectory.exists())
            throw new AssertionError("Project " + projectName + ": non-existant project directory '" + projectDirectory + "'.");
    }

    /**
     * Given the root directory for a project or library, create a repository under the local history directory.
     *
     * @param projectDirectory the root of the project
     * @return the Repository.
     */
    public static Repository create(LocalHistoryManager manager, File projectDirectory) {
        if (!projectDirectory.exists())
            throw new AssertionError("The project directory '" + projectDirectory + "' does not exist.");
        if (!projectDirectory.isDirectory())
            throw new AssertionError("The project directory '" + projectDirectory + "' is not a directory.");
        String projectName = projectDirectory.getName();
        File repositoryDirectory = new File(manager.getLocalHistoryDirectory(), projectName);
        File objectsDirectory = new File(repositoryDirectory, "objects");
        File refsDirectory = new File(repositoryDirectory, "refs");
        File configFile = new File(repositoryDirectory, "config");
        if (repositoryDirectory.exists())
            throw new AssertionError("A repository named '" + projectName + "' already exists.");
        boolean success;
        success = repositoryDirectory.mkdir();
        if (!success)
            throw new RuntimeException("Error while creating repository directory " + repositoryDirectory);
        success = objectsDirectory.mkdir();
        if (!success)
            throw new RuntimeException("Error while creating objects directory " + objectsDirectory);
        success = refsDirectory.mkdir();
        if (!success)
            throw new RuntimeException("Error while creating refs directory " + refsDirectory);
        Properties p = new Properties();
        p.setProperty("repositoryformatversion", "0");
        p.setProperty("projectpath", projectDirectory.getAbsolutePath());
        try {
            configFile.createNewFile();
            FileOutputStream out = new FileOutputStream(configFile);
            p.store(out, null);
        } catch (IOException e) {
            throw new RuntimeException("Error while writing configuration file " + configFile + ".", e);
        }
        return new Repository(manager, projectName);
    }

    //// Getters ////

    public LocalHistoryManager getManager() {
        return manager;
    }

    public String getProjectName() {
        return projectName;
    }

    public File getProjectDirectory() {
        return projectDirectory;
    }

    public File getDirectory() {
        return directory;
    }

    //// Low-level operations ////

    public File objectPath(String id) {
        return objectPath(id, false);
    }

    /**
     * Returns the path of the internal object file in the object database.
     *
     * @param id         the object id.
     * @param createPath create the path if it does not exist yet.
     * @return the path of the object file.
     */
    public File objectPath(String id, boolean createPath) {
        if (id == null || id.length() != 40)
            throw new AssertionError("Invalid id. Use hashObject to get a valid id.");
        // The directory is composed of the two first characters of the hash.
        String firstTwo = id.substring(0, 2);
        String theRest = id.substring(2);
        File dirName = new File(directory, "objects/" + firstTwo);
        if (createPath && !dirName.exists()) {
            if (!dirName.mkdir()) {
                throw new AssertionError("Project " + projectName + ": could not create directory '" + dirName + "'.");
            }
        }
        return new File(dirName, theRest);
    }

    /**
     * Returns true if an object with the given hash exists in the object database.
     *
     * @param id the object id.
     * @return true if the object exists.
     */
    public boolean objectExists(String id) {
        return objectPath(id, false).exists();
    }

    /**
     * Return the data of the repository object with the given id.
     *
     * @param id the object id.
     * @return the data of the object.
     */
    public byte[] readObject(String id) {
        File objectPath = objectPath(id);
        return readFile(objectPath);
//        byte[] compressedData = readFile(objectPath);
//        Inflater inflater = new Inflater();
//        inflater.setInput(compressedData);
//        byte[] dataStream = new byte[10000];
//        try {
//            int decompressedBytes = inflater.inflate(dataStream);
//            inflater.end();
//        } catch (DataFormatException e) {
//            throw new RuntimeException("Data error while decompressing " + id, e);
//        }
    }

    /**
     * Return the object id for the given project file.
     *
     * @param fileName the file to
     * @return the object id.
     */
    public String hashObject(String fileName) {
        return hashObject(fileName, false);
    }

    /**
     * Return the object id for the given project file.
     *
     * @param fileName the file name to hash
     * @param write    if true, store the file into the object database.
     * @return the object id.
     */
    public String hashObject(String fileName, boolean write) {
        File fullPath = new File(projectDirectory, fileName);
        return hashObject(fullPath, write);
    }

    /**
     * Return the object id for the given project file.
     *
     * @param file  the file to hash
     * @param write if true, store the file into the object database.
     * @return the object id.
     */
    public String hashObject(File file, boolean write) {
        if (!file.exists())
            throw new AssertionError("File '" + file + "' does not exist.");
        if (file.isDirectory())
            throw new AssertionError("File '" + file + "' is a directory, which this method cannot hash.");
        byte[] data = readFile(file);
        if (write)
            return writeObject(data);
        else
            return hashData(data);
    }

    /**
     * Write the data to the object database.
     * The object will be stored under its hash, which will be returned.
     *
     * @param data the data of the object
     * @return the object id.
     */
    public String writeObject(byte[] data) {
        String id = hashData(data);
        if (!objectExists(id)) {
            File objectPath = objectPath(id, true);
            writeFile(objectPath, data, 0, data.length);
//            byte[] compressedData = new byte[data.length];
//            Deflater d = new Deflater();
//            d.setInput(data);
//            d.finish();
//            int compressedLength = d.deflate(compressedData);
//            writeFile(objectPath, compressedData, 0, compressedLength);
        }
        return id;
    }

    /**
     * Return the number of objects in the object database.
     *
     * @return the number of objects in the object database.
     */
    public int getObjectCount() {
        File objectsDirectory = new File(getDirectory(), "objects");
        int count = 0;
        for (File objectDir : objectsDirectory.listFiles()) {
            // Only two-character directory names and names without dots (this avoids .svn directories)
            if (objectDir.getName().length() != 2) continue;
            if (objectDir.getName().startsWith(".")) continue;
            // Include only files with 38 characters, that is files with a hex digest name.
            for (File objectFile : objectDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.length() == 38;
                }
            })) {
                count++;
            }
        }
        return count;
    }

    /**
     * Reads a reference from the refs directory.
     *
     * @param name the name of the reference.
     * @return the ID of the reference.
     */
    public String readRef(String name) {
        File refsDirectory = new File(getDirectory(), "refs");
        File refsFile = new File(refsDirectory, name);
        if (!refsFile.exists()) return null;
        byte[] refBytes = readFile(refsFile);
        return new String(refBytes);
    }


    /**
     * Write a reference to the refs directory.
     *
     * @param name the name of the reference.
     * @param id   the id of the object it points to.
     */
    public void writeRef(String name, String id) {
        File refsDirectory = new File(getDirectory(), "refs");
        File refsFile = new File(refsDirectory, name);
        writeFile(refsFile, id.getBytes(), 0, 40);
    }

    /**
     * Checks if the given reference exists.
     *
     * @param name the name of the reference.
     * @return true if the reference exists.
     */
    public boolean refExists(String name) {
        File refsDirectory = new File(getDirectory(), "refs");
        File refsFile = new File(refsDirectory, name);
        return refsFile.exists();
    }

    //// High-level operations ////

    /**
     * Store the contents of the working directory in the object database.
     * This commit will be stored using an empty message.
     *
     * @return the id of the commit object.
     */
    public String commit() {
        return commit("");
    }

    /**
     * Store the contents of the working directory in the object database.
     *
     * @param message the message for the commit
     * @return the id of the commit object.
     */
    public String commit(String message) {
        StringBuffer commitDataBuffer = new StringBuffer();
        // Store the contents of the working directory in the object database.
        // This will return the root id of the tree.
        String treeId = hashDirectoryRecursive(projectDirectory);
        commitDataBuffer.append("tree ").append(treeId).append("\n");
        // Find the current head: this will be the parent of this commit.
        String parentId = readRef("HEAD");
        if (parentId != null) {
            commitDataBuffer.append("parent ").append(parentId).append("\n");
        }
        // Add the commit time
        Calendar cal = Calendar.getInstance();
        commitDataBuffer.append("time ").append(Commit.dateFormat.format(cal.getTime())).append("\n");
        // Append an empty line to indicate the commit message follows
        commitDataBuffer.append("\n");
        // Add the commit message
        commitDataBuffer.append(message);
        // Store the commit in the object database.
        String commitId = writeObject(commitDataBuffer.toString().getBytes());
        //This commit will be the new head. Write a reference to it.
        writeRef("HEAD", commitId);
        return commitId;
    }

    public Commit getHead() {
        String refId = readRef("HEAD");
        if (refId == null)
            return null;
        return new Commit(this, refId);
    }

    //// Utility methods ////

    /**
     * Calculate the id, or hash of the given data.
     *
     * @param data the data
     * @return the object id.
     */
    public String hashData(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("This Java implementation does not support SHA-1 hashing.");
        }
        md.update(data);
        byte[] digest = md.digest();
        String hexStr = "";
        for (byte aDigest : digest) {
            hexStr += Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1);
        }
        return hexStr;
    }

    public String hashDirectoryRecursive(File directory) {
        if (!directory.exists())
            throw new AssertionError("Directory '" + directory + "' does not exist.");
        if (!directory.isDirectory())
            throw new AssertionError("Directory '" + directory + "' is not a directory.");
        StringBuffer treeDataBuffer = new StringBuffer();
        for (File f : directory.listFiles()) {
            String type, id;
            if (f.isDirectory()) {
                id = hashDirectoryRecursive(f);
                type = "tree";
            } else {
                id = hashObject(f, true);
                type = "blob";
            }
            treeDataBuffer.append(type).append(" ").append(id).append("\t").append(f.getName()).append("\n");
        }
        // Remove the final "\n"
        String treeData = treeDataBuffer.substring(treeDataBuffer.length() - 1);
        return writeObject(treeData.getBytes());
    }


    /**
     * Read in a file as a stream of bytes.
     *
     * @param file the file
     * @return a bytestream with the data of the file.
     */
    private byte[] readFile(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            long fileSize = file.length();
            // Check if the file is too large
            // (not larger than maximum value of an integer, which is the maximum size for a byte array).
            byte[] bytes = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length &&
                    (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            // Ensure all bytes have been read.
            if (offset < bytes.length)
                throw new RuntimeException("Could not completely read file " + file);
            in.close();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + file, e);
        }
    }

    /**
     * Write a byte array to a file.
     *
     * @param file   the file
     * @param data   a bytestream with the data of the file.
     * @param offset the offset in the data stream.
     * @param length the length of the data stream.
     */
    private void writeFile(File file, byte[] data, int offset, int length) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(data, offset, length);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + file, e);
        }
    }
}
