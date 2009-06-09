package nodebox.localhistory;

import java.io.File;
import java.util.HashMap;

public class LocalHistoryManager {

    private File localHistoryDirectory;
    private HashMap<String, Repository> repositories = new HashMap<String, Repository>();

    public LocalHistoryManager(File localHistoryDirectory) {
        this.localHistoryDirectory = localHistoryDirectory;
        boolean success = localHistoryDirectory.mkdirs();
        if (!success)
            throw new RuntimeException("Error while creating local history directory " + localHistoryDirectory);
        if (!localHistoryDirectory.isDirectory())
            throw new AssertionError("Local history directory " + localHistoryDirectory + " does not exist.");
        loadRepositories();
    }

    public File getLocalHistoryDirectory() {
        return localHistoryDirectory;
    }

    public Repository createRepository(File projectDirectory) {
        Repository r = Repository.create(this, projectDirectory);
        repositories.put(r.getProjectName(), r);
        return r;
    }

    private void loadRepositories() {
        for (File projectDirectory : localHistoryDirectory.listFiles()) {
            if (!projectDirectory.isDirectory()) continue;
            String projectName = projectDirectory.getName();
            Repository r = new Repository(this, projectName);
            repositories.put(projectName, r);
        }
    }
}
