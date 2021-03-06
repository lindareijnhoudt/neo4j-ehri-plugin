package eu.ehri.extension;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.acl.GlobalPermissionSet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.Sets;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.AclViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 * <p/>
 * TODO: These functions will typically be called quite frequently for the
 * portal. We should possibly implement some kind of caching system for ACL
 * permissions.
 */
@Path(Entities.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    private final ObjectMapper mapper = new ObjectMapper();

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list/{id:.+}")
    public StreamingOutput listPermissionGrants(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(user.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page/{id:.+}")
    public StreamingOutput pagePermissionGrants(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(SORT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(user.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForItem/{id:.+}")
    public StreamingOutput listPermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/pageForItem/{id:.+}")
    public StreamingOutput pagePermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForScope/{id:.+}")
    public StreamingOutput listPermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/pageForScope/{id:.+}")
    public StreamingOutput pagePermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response getGlobalMatrix() throws PermissionDenied, IOException,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(accessor.getId());
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}")
    public Response getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, IOException, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(stringifyInheritedGlobalMatrix(acl
                                .getInheritedGlobalPermissions(accessor))))
                        .build();
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId
     * @param json
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}")
    public Response setGlobalMatrix(@PathParam("userId") String userId,
            String json) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        HashMap<String, List<String>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        try {
            new AclViews(graph).setGlobalPermissionMatrix(accessor,
                    enumifyMatrix(globals), grantee);
            graph.getBaseGraph().commit();
            return getGlobalMatrix(userId);
        } catch (PermissionDenied permissionDenied) {
            graph.getBaseGraph().rollback();
            throw permissionDenied;
        } catch (DeserializationError deserializationError) {
            graph.getBaseGraph().rollback();
            throw deserializationError;
        } catch (IOException e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (ItemNotFound itemNotFound) {
            graph.getBaseGraph().rollback();
            throw itemNotFound;
        }
    }

    /**
     * Get the permission matrix for a given user on the given entity.
     *
     * @param userId
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/{id:.+}")
    public Response getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, IOException,
            ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = new AclManager(graph, entity.getPermissionScope());

        return Response
                .status(Response.Status.OK)
                .entity(mapper.writeValueAsBytes(stringifyInheritedMatrix(acl
                        .getInheritedEntityPermissions(accessor, entity))))
                        .build();
    }

    /**
     * Get the user's permissions for a given scope.
     *
     * @param userId
     * @param id
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     * @throws DeserializationError
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public Response getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound,
            IOException, DeserializationError {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = new AclManager(graph, scope);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(stringifyInheritedGlobalMatrix(acl
                                .getInheritedGlobalPermissions(accessor))))
                .build();
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId
     *            the user
     * @param id
     *            the scope id
     * @param json
     *            the serialized permission list
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/scope/{id:.+}")
    public Response setScopedPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id, String json) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();

        try {
            HashMap<String, List<String>> globals = parseMatrix(json);
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph, scope);
            acl.setGlobalPermissionMatrix(accessor, enumifyMatrix(globals), grantee);
            graph.getBaseGraph().commit();
            return getScopedMatrix(userId, id);
        } catch (IOException e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (DeserializationError deserializationError) {
            graph.getBaseGraph().rollback();
            throw deserializationError;
        } catch (ItemNotFound itemNotFound) {
            graph.getBaseGraph().rollback();
            throw itemNotFound;
        } catch (PermissionDenied permissionDenied) {
            graph.getBaseGraph().rollback();
            throw permissionDenied;
        }
    }

    /**
     * Set a user's permissions on a given item.
     *
     * @param id
     *            the item id
     * @param userId
     *            the user id
     * @param json
     *            the serialized permission list
     * @return
     *
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/{id:.+}")
    public Response setItemPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id, String json) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        List<String> scopedPerms;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            scopedPerms = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }

        Accessor accessor = manager.getFrame(userId, Accessor.class);

        try {
            AccessibleEntity item = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph);

            acl.setItemPermissions(item, accessor,
                    enumifyPermissionList(scopedPerms), grantee);
            graph.getBaseGraph().commit();
            return Response
                    .status(Response.Status.OK)
                    .entity(mapper
                            .writeValueAsBytes(stringifyInheritedMatrix(new AclManager(
                                    graph).getInheritedEntityPermissions(accessor,
                                    manager.getFrame(id, AccessibleEntity.class)))))
                    .build();
        } catch (ItemNotFound itemNotFound) {
            graph.getBaseGraph().rollback();
            throw itemNotFound;
        } catch (PermissionDenied permissionDenied) {
            graph.getBaseGraph().rollback();
            throw permissionDenied;
        } catch (DeserializationError deserializationError) {
            graph.getBaseGraph().rollback();
            throw deserializationError;
        } catch (IOException e) {
            graph.getBaseGraph().rollback();
            throw e;
        }
    }

    // Helpers. These just convert from string to internal enum representations
    // of the various permissions-related data structures.
    // TODO: There's probably a way to get Jackson to do that automatically.
    // This was why scala was invented...

    private HashMap<String, List<String>> parseMatrix(String json)
            throws IOException, DeserializationError {
        HashMap<String, List<String>> globals;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<HashMap<String, List<String>>> typeRef = new TypeReference<HashMap<String, List<String>>>() {
            };
            globals = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
        return globals;
    }

    private List<Map<String, Map<String, List<String>>>> stringifyInheritedGlobalMatrix(
            List<Map<String, GlobalPermissionSet>> list2) {
        List<Map<String, Map<String, List<String>>>> list = Lists
                .newLinkedList();
        for (Map<String, GlobalPermissionSet> item : list2) {
            Map<String, Map<String, List<String>>> tmp = Maps.newHashMap();
            for (Map.Entry<String, GlobalPermissionSet> entry : item.entrySet()) {
                tmp.put(entry.getKey(), stringifyGlobalMatrix(entry.getValue().asMap()));
            }
            list.add(tmp);
        }
        return list;
    }

    private Map<String, List<String>> stringifyGlobalMatrix(
            Map<ContentTypes, Collection<PermissionType>> map) {
        Map<String, List<String>> tmp = Maps.newHashMap();
        for (Map.Entry<ContentTypes, Collection<PermissionType>> entry : map
                .entrySet()) {
            List<String> ptmp = Lists.newLinkedList();
            for (PermissionType pt : entry.getValue()) {
                ptmp.add(pt.getName());
            }
            tmp.put(entry.getKey().getName(), ptmp);
        }
        return tmp;
    }

    private List<Map<String, List<String>>> stringifyInheritedMatrix(
            List<Map<String, List<PermissionType>>> matrix) {
        List<Map<String, List<String>>> tmp = Lists.newLinkedList();
        for (Map<String, List<PermissionType>> item : matrix) {
            tmp.add(stringifyMatrix(item));
        }
        return tmp;
    }

    private Map<String, List<String>> stringifyMatrix(
            Map<String, List<PermissionType>> matrix) {
        Map<String, List<String>> out = Maps.newHashMap();
        for (Map.Entry<String, List<PermissionType>> entry : matrix.entrySet()) {
            List<String> tmp = Lists.newLinkedList();
            for (PermissionType t : entry.getValue()) {
                tmp.add(t.getName());
            }
            out.put(entry.getKey(), tmp);
        }
        return out;
    }

    /**
     * Convert a permission matrix containing strings in lieu of content type
     * and permission type enum values to the enum version. If Jackson 1.9 were
     * available in Neo4j we wouldn't need this, since its @JsonCreator
     * annotation allows specifying how to deserialize those enums properly.
     *
     * @param matrix
     * @return
     * @throws DeserializationError
     */
    private Map<ContentTypes, List<PermissionType>> enumifyMatrix(
            Map<String, List<String>> matrix) throws DeserializationError {
        try {
            Map<ContentTypes, List<PermissionType>> out = Maps.newHashMap();
            for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
                List<PermissionType> tmp = Lists.newLinkedList();
                for (String t : entry.getValue()) {
                    tmp.add(PermissionType.withName(t));
                }
                out.put(ContentTypes.withName(entry.getKey()), tmp);
            }
            return out;
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    private Set<PermissionType> enumifyPermissionList(List<String> scopedPerms)
            throws DeserializationError {
        try {
            Set<PermissionType> perms = Sets.newHashSet();
            for (String p : scopedPerms) {
                perms.add(PermissionType.withName(p));
            }

            return perms;
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}