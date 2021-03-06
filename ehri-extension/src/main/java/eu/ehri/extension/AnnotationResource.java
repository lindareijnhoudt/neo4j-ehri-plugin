package eu.ehri.extension;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.*;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.ListMultimap;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.AnnotationViews;

/**
 * Provides a RESTfull(ish) interface for creating.
 */
@Path(Entities.ANNOTATION)
public class AnnotationResource extends
        AbstractAccessibleEntityResource<Annotation> {

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
    }

    /**
     * Retrieve an annotation by id.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}"
    )
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List all annotations.
     * 
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listAnnotations(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }
    
    /**
     * Create an annotation for a particular item.
     * 
     * @param id
     * @param json
     * @param accessors
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response createAnnotationFor(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            Annotation ann = new AnnotationViews(graph).createFor(id,
                    Bundle.fromString(json), user);
            new AclManager(graph).setAccessors(ann,
                    getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return buildResponseFromAnnotation(ann);
        } catch (ItemNotFound e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (PermissionDenied e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (DeserializationError e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (BadRequester e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (ValidationError e) {
            graph.getBaseGraph().rollback();
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new WebApplicationException(e);
        }
    }

    /**
     * Return a map of annotations for the subtree of the given item and its
     * child items.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public StreamingOutput listAnnotationsForSubtree(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, PermissionDenied {
        AnnotationViews annotationViews = new AnnotationViews(graph);
        ListMultimap<String, Annotation> anns = annotationViews.getFor(id,
                getRequesterUserProfile());
        return streamingMultimap(anns);
    }

    private Response buildResponseFromAnnotation(Annotation ann)
            throws SerializationError {
        String jsonStr = serializer.vertexFrameToJson(ann);
        return Response.status(Status.CREATED).entity((jsonStr).getBytes())
                .build();
    }

    /**
     * Delete an annotation.
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteAnnotation(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
