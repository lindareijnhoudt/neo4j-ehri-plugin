package eu.ehri.project.views;

import java.util.Iterator;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;

public class CrudViewsTest extends AbstractFixtureTest {

    /**
     * Access an item 0 as user 20.
     * 
     * @throws AccessDenied
     */
    @Test
    public void testDetail() throws AccessDenied {
        Crud<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        DocumentaryUnit unit = docViews.detail(item, validUser);
        assertEquals(item.asVertex(), unit.asVertex());
    }

    /**
     * Check we can access the user's own profile.
     * 
     * @throws AccessDenied
     */
    @Test
    public void testUserProfile() throws AccessDenied {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        UserProfile user = userViews.detail(validUser, validUser);
        assertEquals(validUser.asVertex(), user.asVertex());
    }

    /**
     * Access an item as an anon user. This should throw PermissionDenied
     * 
     * @throws AccessDenied
     */
    @Test(expected = AccessDenied.class)
    public void testDetailAnonymous() throws AccessDenied {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        docViews.detail(item, AnonymousAccessor.getInstance());
    }

    /**
     * Access an item as an invalid user.
     * 
     * @throws AccessDenied
     */
    @Test(expected = AccessDenied.class)
    public void testDetailPermissionDenied() throws AccessDenied {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        docViews.detail(item, invalidUser);
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test(expected = PermissionDenied.class)
    public void testCreateAsUnauthorized() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, invalidUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testCreateAsUnauthorizedAndThenGrant() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError,
            ItemNotFound {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());

        try {
            docViews.create(bundle, invalidUser);
            fail("Creation should throw "
                    + PermissionDenied.class.getSimpleName());
        } catch (PermissionDenied e) {
            // We expected that permission denied... now explicitely add
            // permissions.
            PermissionGrantTarget target = manager.getFrame(
                    ContentTypes.DOCUMENTARY_UNIT.getName(),
                    PermissionGrantTarget.class);
            new AclManager(graph).grantPermissions(invalidUser, target,
                    PermissionType.CREATE);
            DocumentaryUnit unit = docViews.create(bundle, invalidUser);
            assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));
        }
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testCreateWithScope() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError, ItemNotFound {
        Crud<DocumentaryUnit> docViews = new LoggingCrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class, manager.getFrame("r1",
                        Repository.class));
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        // In the fixtures, 'reto' should have a grant for 'CREATE'
        // scoped to the 'r1' repository.
        DocumentaryUnit unit = docViews.create(bundle, invalidUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));
    }

    /**
     * Access the valid user's profile as an invalid users.
     * @throws AccessDenied
     *
     */
    public void testUserDetailAccessDenied() throws AccessDenied {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        userViews.detail(validUser, invalidUser);
    }

    /**
     * Test updating an item.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        String newName = TestData.TEST_COLLECTION_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(unit.getId()).withDataValue(
                "name", newName);

        DocumentaryUnit changedUnit = docViews.update(newBundle, validUser).getNode();
        assertEquals(newName, changedUnit.asVertex().getProperty("name"));
        DocumentDescription desc = graph.frame(
                changedUnit.getDescriptions().iterator().next().asVertex(),
                DocumentDescription.class);

        // Check the nested item was created correctly
        DatePeriod datePeriod = desc.getDatePeriods().iterator().next();
        assertTrue(datePeriod != null);
        assertEquals(TestData.TEST_START_DATE, datePeriod.getStartDate());

        // And that the reverse relationship works.
        assertEquals(desc.asVertex(), datePeriod.getEntity().asVertex());
    }

    /**
     * Test updating a user.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testUserUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        Bundle bundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TestData.TEST_USER_NAME, user.getName());

        String newName = TestData.TEST_USER_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(user.getId()).withDataValue(
                "name", newName);
        UserProfile changedUser = userViews.update(newBundle, validUser).getNode();
        assertEquals(newName, changedUser.getName());
    }

    /**
     * Test we can create a new user.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testUserCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        Bundle bundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TestData.TEST_USER_NAME, user.getName());
    }

    /**
     * Test we can create a new group.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testGroupCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        CrudViews<Group> groupViews = new CrudViews<Group>(graph, Group.class);
        Bundle bundle = Bundle.fromData(TestData.getTestGroupBundle());
        Group group = groupViews.create(bundle, validUser);
        assertEquals(TestData.TEST_GROUP_NAME, group.getName());
    }

    /**
     * Test creating a view with invalid data throws a validationError
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testCreateWithError() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .removeDataValue("name");

        // This shouldn't barf because the collection does not need a name.
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(null, unit.asVertex().getProperty("name"));
    }

    /**
     * Tests that deleting a collection will also delete its dependent
     * relations. NB: This test will break of other @Dependent relations are
     * added to DocumentaryUnit.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    @Test
    public void testDelete() throws PermissionDenied, ValidationError,
            SerializationError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Integer shouldDelete = 1;

        // FIXME: Surely there's a better way of doing this???
        Iterator<Description> descIter = item.getDescriptions().iterator();
        for (; descIter.hasNext(); shouldDelete++) {
            DocumentDescription d = graph.frame(descIter.next().asVertex(), DocumentDescription.class);
            for (DatePeriod dp : d.getDatePeriods()) shouldDelete++;
            for (UndeterminedRelationship r : d.getUndeterminedRelationships()) shouldDelete++;
        }

        Integer deleted = docViews.delete(item, validUser);
        assertEquals(shouldDelete, deleted);
    }


    /**
     * Test updating an item's dependent item.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testCreateDependent() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound, SerializationError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        long descCount = Iterables.count(unit.getDescriptions());
        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.IDENTIFIER_KEY, "some-new-id");

        DocumentDescription changedDesc = docViews.createDependent(descBundle, unit, validUser,
                DocumentDescription.class);
        unit.addDescription(changedDesc);

        // The order in which items are serialized is undefined, so we just have to throw
        // an error if we don't fine the right item...
        for (Bundle b : new Serializer(graph)
                     .vertexFrameToBundle(unit).getRelations(Ontology
                        .DESCRIPTION_FOR_ENTITY)) {
            if (b.getDataValue(Ontology.IDENTIFIER_KEY).equals("some-new-id")) {
                return;
            }
        }
        throw new RuntimeException("Item does not have description with identifier: some-new-id");
    }

    /**
     * Test updating an item's dependent item.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testUpdateDependent() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound, SerializationError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());
        Bundle descBundle = new Serializer(graph).vertexFrameToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.NAME_KEY, "some-new-title");

        DocumentDescription changedDesc = docViews.updateDependent(descBundle, unit, validUser,
                DocumentDescription.class).getNode();
        assertEquals(descCount, Iterables.count(unit.getDocumentDescriptions()));
        assertEquals("some-new-title", changedDesc.getName());
    }

    /**
     * Test deleting an item's child item.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testDeleteDependent() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound, SerializationError {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());

        DocumentDescription d = Iterables.first(unit.getDocumentDescriptions());
        assertNotNull(d);
        int delCount = docViews.deleteDependent(d, unit, validUser, DocumentDescription.class);
        assertTrue(delCount >= 1);
        assertEquals(descCount - 1, Iterables.count(unit.getDocumentDescriptions()));
    }
}
