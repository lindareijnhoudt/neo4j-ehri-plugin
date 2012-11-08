package eu.ehri.project.views;

import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Converter;

abstract class AbstractViews<E extends AccessibleEntity> {

    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;
    /**
     * Default scope for Permission operations is the system, but this can be
     * overridden.
     */
    protected PermissionScope scope = new SystemScope();

    /**
     * @param graph
     * @param cls
     */
    public AbstractViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }

    /**
     * Check permissions for a given type.
     * 
     * @throws PermissionDenied
     */
    public void checkPermission(Accessor accessor, Permission permission)
            throws PermissionDenied {
        // If we're admin, the answer is always "no problem"!
        if (!acl.belongsToAdmin(accessor)) {
            ContentType contentType = getContentType(ClassUtils
                    .getEntityType(cls));
            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    contentType, permission);
            boolean found = false;
            for (PermissionGrant perm : perms) {
                // If the permission has unscoped rights, the user is
                // good to do whatever they want to do here.
                PermissionScope permScope = perm.getScope();
                if (permScope == null
                        || permScope.asVertex().equals(scope.asVertex())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new PermissionDenied(accessor, contentType, permission,
                        scope);
            }
        }
    }

    /**
     * Check permissions for a given entity.
     * 
     * @throws PermissionDenied
     */
    public void checkEntityPermission(AccessibleEntity entity,
            Accessor accessor, Permission permission) throws PermissionDenied {

        // TODO: Determine behaviour for granular item-level
        // attributes.
        try {
            checkPermission(accessor, permission);
        } catch (PermissionDenied e) {
            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    entity, permission);
            // Scopes do not apply to entity-level perms...
            if (!perms.iterator().hasNext())
                throw new PermissionDenied(accessor, entity);
        }

    }

    /**
     * Ensure an item is readable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkReadAccess(AccessibleEntity entity, Accessor user)
            throws PermissionDenied {
        if (!acl.getAccessControl(entity, user))
            throw new PermissionDenied(user, entity);
    }

    /**
     * Ensure an item is writable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, Accessor accessor)
            throws PermissionDenied {
        checkEntityPermission(entity, accessor,
                getPermission(PermissionTypes.UPDATE));
    }

    /**
     * Get the access with the given id, or the special anonymous access
     * otherwise.
     * 
     * @param id
     * @return
     */
    protected Accessor getAccessor(Accessor accessor) {
        return accessor != null ? graph.frame(accessor.asVertex(),
                Accessor.class) : new AnonymousAccessor();
    }

    /**
     * Get the content type with the given id.
     * 
     * @param typeName
     * @return
     */
    public ContentType getContentType(String typeName) {
        try {
            return graph
                    .getVertices(AccessibleEntity.IDENTIFIER_KEY, typeName,
                            ContentType.class).iterator().next();
        } catch (NoSuchElementException e) {
            throw new RuntimeException(String.format(
                    "No content type node found for type: '%s'", typeName), e);
        }
    }

    /**
     * Fetch any item of a particular type by its identifier.
     * 
     * @param typeName
     * @param name
     * @param cls
     * @return
     * @throws ItemNotFound
     */
    public <T> T getEntity(String typeName, String name, Class<T> cls)
            throws ItemNotFound {
        // FIXME: Ensure index isn't null
        Index<Vertex> index = graph.getBaseGraph().getIndex(typeName,
                Vertex.class);

        CloseableIterable<Vertex> query = index.get(
                AccessibleEntity.IDENTIFIER_KEY, name);
        try {
            return graph.frame(query.iterator().next(), cls);
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(AccessibleEntity.IDENTIFIER_KEY, name);
        } finally {
            query.close();
        }
    }

    /**
     * Get the permission with the given string.
     * 
     * @param permissionId
     * @return
     */
    public Permission getPermission(String permissionId) {
        try {
            return graph
                    .getVertices(AccessibleEntity.IDENTIFIER_KEY, permissionId,
                            Permission.class).iterator().next();
        } catch (NoSuchElementException e) {
            throw new RuntimeException(String.format(
                    "No permission found for name: '%s'", permissionId), e);
        }
    }

    /**
     * Set the scope under which ACL and permission operations will take place.
     * This is, for example, an Agent instance, where the objects being
     * manipulated are DocumentaryUnits. The given scope is used to compare
     * against the scope relation on PermissionGrants.
     * 
     * @param scope
     */
    public void setScope(PermissionScope scope) {
        this.scope = scope;
    }
}
