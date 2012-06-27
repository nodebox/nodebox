package nodebox.node;

import nodebox.util.FileUtils;
import org.python.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The result of NodeLibrary.upgrade().
 * <p/>
 * Upgrades can fail (in which case a LoadException is thrown), or pass but with warnings.
 * These warnings are stored in the UpgradeResult.
 */
public class UpgradeResult {

    private final File file;
    private final String xml;
    private final List<String> warnings;

    public UpgradeResult(File file, String xml, List<String> warnings) {
        checkNotNull(file);
        checkNotNull(xml);
        checkNotNull(warnings);
        this.file = file;
        this.xml = xml;
        this.warnings = ImmutableList.copyOf(warnings);
    }

    /**
     * The file that was upgraded.
     * @return The upgraded file.
     */
    public File getFile() {
        return file;
    }

    /**
     * The upgraded XML code, as a String.
     * @return The upgraded XML code.
     */
    public String getXml() {
        return xml;
    }

    /**
     * The list of warnings that occurred during upgrades.
     * @return The list of warnings. The list is immutable.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Get the upgraded library. This parses the upgraded XML into a new library format.
     *
     * @param baseFile The old ndbx file. This is used for loading libraries relative from the file.
     * @param nodeRepository The node repository to load nodes out of.
     * @return The new NodeLibrary.
     */
    public NodeLibrary getLibrary(File baseFile, NodeRepository nodeRepository) {
        String libraryName = FileUtils.stripExtension(getFile());
        return NodeLibrary.load(libraryName, xml, baseFile, nodeRepository);
    }

}
