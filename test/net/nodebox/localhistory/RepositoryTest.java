package net.nodebox.localhistory;

import junit.framework.TestCase;
import net.nodebox.util.FileUtils;

import java.io.File;

public class RepositoryTest extends TestCase {

    private File tempDirectory;
    private File localHistoryDirectory;
    private File testDirectory;
    private LocalHistoryManager manager;

    @Override
    protected void setUp() throws Exception {
        // Create a tempory folder
        tempDirectory = File.createTempFile("localhistory", "");
        tempDirectory.delete();
        tempDirectory.mkdir();
        localHistoryDirectory = new File(tempDirectory, "_history");
        manager = new LocalHistoryManager(localHistoryDirectory);
        testDirectory = new File(tempDirectory, "testproject");
        testDirectory.mkdir();
    }

    /**
     * Basic sanity test to see if object hashing works.
     */
    public void testHashObject() {
        Repository r = manager.createRepository(testDirectory);
        String fname = "greeting";
        String contents = "Hello, world!";
        createProjectFile(fname, contents);
        assertEquals("943a702d06f34599aee1f8da8ef9f7296031d699", r.hashObject("greeting"));
    }

    public void testCreate() {
        Repository r = manager.createRepository(testDirectory);
        // Creating the repository should have created the _history/testproject folder
        // and a configuration file inside of that folder.
        File projectHistoryDirectory = new File(localHistoryDirectory, "testproject");
        File configFile = new File(projectHistoryDirectory, "config");
        assertTrue(projectHistoryDirectory.exists() && projectHistoryDirectory.isDirectory());
        assertTrue(configFile.exists() && configFile.isFile());
        assertEquals(testDirectory, r.getProjectDirectory());
        // Trying to create the repository again will result in an error.
        try {
            manager.createRepository(testDirectory);
            fail("Should have thrown an error.");
        } catch (AssertionError ignored) {
        }
    }

    /**
     * Test adding files to the project and see if they show up in the repository.
     */
    public void testAddFile() {
        Repository r = manager.createRepository(testDirectory);
        // Assert this repository is empty: no objects, and the head commit returns None.
        assertEquals(0, r.getObjectCount());
        assertEquals(null, r.getHead());
        String fname = "greeting";
        String contents = "Hello, world!";
        createProjectFile(fname, contents);
        String id = r.hashObject(fname);
        String commitId1 = r.commit("Adding files to the project.");
        // Assert that the contents are stored in the repository.
        assertEquals(contents, new String(r.readObject(id)));
        // Assert the head commit now refers to the newly created commit.
        assertEquals(commitId1, r.getHead().getId());
        // Since this is the first commit, it will not have a parent.
        assertEquals(null, r.getHead().getParentId());
        // Once commited, there will be three objects in the database: the file, the tree, and the commit.
        assertEquals(3, r.getObjectCount());
        String commitId2 = r.commit("My second commit with the same files.");
        assertNotSame(commitId1, commitId2);
        // Since no files/folders were changed, only one new object is created in the database: the second commit.
        assertEquals(4, r.getObjectCount());
        // Assert the head commit now refers to the newly created commit.
        assertEquals(commitId2, r.getHead().getId());
        // Assert the parent commit refers to our previous commit.
        assertEquals(commitId1, r.getHead().getParentId());
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirectory);
    }

    private void createProjectFile(String fname, String contents) {
        File projectFile = new File(testDirectory, fname);
        FileUtils.writeFile(projectFile, contents);
    }

}
