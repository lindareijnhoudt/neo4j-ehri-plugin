package eu.ehri.project.utils.fixtures;

import java.io.InputStream;

/**
 * Interface for classes which handle fixture loading.
 * 
 * @author mike
 *
 */
public interface FixtureLoader {

    /**
     * Toggle whether or not initialization occurs before
     * loading (default: true)
     */
    public void setInitializing(boolean toggle);

    /**
     * Load the default fixtures.
     */
    public void loadTestData();

    /**
     * Load a given InputStream as test data. The stream
     * will be closed automatically.
     * @param inputStream
     */
    public void loadTestData(InputStream inputStream);

    /**
     * Load a given file as test data.
     *
     * @param resourceNameOrPath
     */
    public void loadTestData(String resourceNameOrPath);
}
