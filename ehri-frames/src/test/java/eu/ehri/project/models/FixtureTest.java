package eu.ehri.project.models;

import java.util.List;

import com.google.common.collect.Iterables;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Accessor;

import static org.junit.Assert.*;

public class FixtureTest extends ModelTestBase {
    @Test
    public void testTheFixturesLoaded() {
        assertTrue(graph.getVertices().iterator().hasNext());
    }

    @Test
    public void testTheGraphContainsGroups() throws ItemNotFound {
        Iterable<Group> frames = manager.getFrames(EntityClass.GROUP,
                Group.class);
        List<Group> groups = toList(frames);
        assertFalse(groups.isEmpty());

        // Check the admin group has some members
        List<Accessor> users = toList(manager.getFrame("admin", Group.class)
                .getMembers());
        assertFalse(users.isEmpty());
    }
}
