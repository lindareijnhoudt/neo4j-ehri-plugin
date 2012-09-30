package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.Query;

public class QueryTest extends AbstractFixtureTest {

    @Test
    public void testAdminCanListEverything() {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        Accessor accessor = graph.frame(graph.getVertex(validUserId),
                Accessor.class);
        assertTrue(new AclManager(graph).isAdmin(accessor));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = graph.getVertices(EntityType.KEY,
                EntityTypes.DOCUMENTARY_UNIT);

        // And the listing the ACL way...
        List<DocumentaryUnit> list = toList(query.list(validUserId));

        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testUserCannotListPrivate() {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        Accessor accessor = helper.getTestFrame("reto", Accessor.class);
        DocumentaryUnit cantRead = helper.getTestFrame("c1",
                DocumentaryUnit.class);
        assertFalse(new AclManager(graph).isAdmin(accessor));

        List<DocumentaryUnit> list = toList(query.list((Long) accessor
                .asVertex().getId()));
        assertFalse(list.isEmpty());
        assertFalse(list.contains(cantRead));
    }

    @Test
    public void testListWithFilter() {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "c1", validUserId));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testListWithGlobFilter() {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = graph.getVertices(EntityType.KEY,
                EntityTypes.DOCUMENTARY_UNIT);

        // Query for document identifier starting with 'c'.
        // In the fixtures this is ALL docs
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "c*", validUserId));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testListWithFailFilter() {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Do a query that won't match anything.
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "__GONNAFAIL__", validUserId));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }
    
    @Test
    public void testGet() throws PermissionDenied, ItemNotFound {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        DocumentaryUnit doc = query.get(AccessibleEntity.IDENTIFIER_KEY, "c1", validUserId);
        assertEquals("c1", doc.getIdentifier());
    }
    
    @Test(expected=ItemNotFound.class)
    public void testGetItemNotFound() throws PermissionDenied, ItemNotFound {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        query.get(AccessibleEntity.IDENTIFIER_KEY, "IDONTEXIST", validUserId);
    }
    
    @Test(expected=PermissionDenied.class)
    public void testGetPermissionDenied() throws PermissionDenied, ItemNotFound {
        Accessor accessor = helper.getTestFrame("reto", Accessor.class);
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        query.get(AccessibleEntity.IDENTIFIER_KEY, "c1", (Long)accessor.asVertex().getId());
    }
}