package eu.ehri.project.persistance;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.utils.BundleUtils;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *
 * TODO: Cover class more comprehensively.
 */
public class SerializerTest extends AbstractFixtureTest {

    @Test
    public void testDefaultSerialization() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);

        Bundle serialized = Serializer.defaultSerializer(graph)
                .vertexFrameToBundle(doc);

        // Name of repository should be serialized
        assertEquals("Documentary Unit 1",
                BundleUtils.get(serialized, "describes[0]/name"));
        assertNotNull(BundleUtils.get(serialized, "describes[0]/scopeAndContent"));
        assertEquals("NIOD Description",
                BundleUtils.get(serialized, "heldBy[0]/describes[0]/name"));

        // But the address data shouldn't
        try {
            BundleUtils.get(serialized, "heldBy[0]/describes[0]/hasAddress[0]/streetAddress");
            fail("Default serializer should not serialize addresses in repository descriptions");
        } catch (BundleUtils.BundlePathError e) {
        }

    }

    @Test
    public void testLiteSerialization() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);

        Bundle serialized = Serializer.liteSerializer(graph)
                .vertexFrameToBundle(doc);

        // Name of doc and repository should be serialized
        assertEquals("Documentary Unit 1",
                BundleUtils.get(serialized, "describes[0]/name"));
        // Not mandatory properties should be null...
        assertNull(BundleUtils.get(serialized, "describes[0]/scopeAndContent"));

        assertEquals("NIOD Description",
                BundleUtils.get(serialized, "heldBy[0]/describes[0]/name"));

    }
}
