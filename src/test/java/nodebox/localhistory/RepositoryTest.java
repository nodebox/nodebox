package nodebox.localhistory;

import nodebox.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;

public class RepositoryTest {

    private File tempDirectory;
    private File localHistoryDirectory;
    private File testDirectory;
    private LocalHistoryManager manager;

    @Before
    public void setUp() throws Exception {
        // Create a temporary folder
        tempDirectory = File.createTempFile("localhistory", "");
        assertTrue(tempDirectory.delete());
        assertTrue(tempDirectory.mkdir());
        localHistoryDirectory = new File(tempDirectory, "_history");
        manager = new LocalHistoryManager(localHistoryDirectory);
        testDirectory = new File(tempDirectory, "testproject");
        assertTrue(testDirectory.mkdir());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirectory);
    }

    /**
     * Basic sanity test to see if object hashing works.
     */
    @Test
    public void testHashObject() {
        Repository r = manager.createRepository(testDirectory);
        String fileName = "greeting";
        String contents = "Hello, world!";
        createProjectFile(fileName, contents);
        assertEquals("943a702d06f34599aee1f8da8ef9f7296031d699", r.hashObject("greeting"));
    }

    @Test
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
    @Test
    public void testAddFile() {
        Repository r = manager.createRepository(testDirectory);
        // Assert this repository is empty: no objects, and the head commit returns None.
        assertEquals(0, r.getObjectCount());
        assertEquals(null, r.getHead());
        String fileName = "greeting";
        String contents = "Hello, world!";
        createProjectFile(fileName, contents);
        String id = r.hashObject(fileName);
        String commitId1 = r.commit("Adding files to the project.");
        // Assert that the contents are stored in the repository.
        assertEquals(contents, new String(r.readObject(id)));
        // Assert the head commit now refers to the newly created commit.
        assertEquals(commitId1, r.getHead().getId());
        // Since this is the first commit, it will not have a parent.
        assertEquals(null, r.getHead().getParentId());
        // Once committed, there will be three objects in the database: the file, the tree, and the commit.
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

    private void createProjectFile(String fileName, String contents) {
        File projectFile = new File(testDirectory, fileName);
        FileUtils.writeFile(projectFile, contents);
    }

}
