package nodebox.client;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExamplesBrowserTest {

    @Test
    public void testCategoriesFollowFolderNumbers() {
        List<ExamplesBrowser.Category> categories = ExamplesBrowser.parseCategories(new File("examples"));
        List<String> names = new ArrayList<String>();
        for (ExamplesBrowser.Category category : categories) {
            names.add(category.name);
        }
        // "01 Basics" comes before "02 Topics", so Basics is the first tab and is selected when the browser opens.
        assertEquals("Basics", names.get(0));
        assertEquals("Topics", names.get(1));

        List<String> basics = new ArrayList<String>();
        for (ExamplesBrowser.SubCategory subCategory : categories.get(0).subCategories) {
            basics.add(subCategory.name);
        }
        assertEquals("Shape", basics.get(0));
        assertEquals("Color", basics.get(1));
    }
}
