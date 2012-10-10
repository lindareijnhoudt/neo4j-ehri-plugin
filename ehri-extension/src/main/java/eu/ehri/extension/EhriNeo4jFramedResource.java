package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.views.ActionViews;
import eu.ehri.project.views.IViews;
import eu.ehri.project.views.Query;

import static eu.ehri.extension.RestHelpers.*;

/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 * 
 * @param <E>
 *            The specific AccesibleEntity derived class
 */
public class EhriNeo4jFramedResource<E extends AccessibleEntity> {

	/**
	 * With each request the headers of that request are injected into the
	 * requestHeaders parameter.
	 */
	@Context
	private HttpHeaders requestHeaders;

	/**
	 * With each request URI info is injected into the uriInfo parameter.
	 */
	@Context
	protected UriInfo uriInfo;

	protected final GraphDatabaseService database;
	protected final FramedGraph<Neo4jGraph> graph;
	protected final IViews<E> views;
	protected final Query<E> querier;
	protected final Class<E> cls;
	protected final Converter converter = new Converter();

	public final static String AUTH_HEADER_NAME = "Authorization";

	/**
	 * Constructor
	 * 
	 * @param database
	 *            Injected neo4j database
	 * @param cls
	 *            The 'entity' class
	 */
	public EhriNeo4jFramedResource(@Context GraphDatabaseService database,
			Class<E> cls) {
		this.database = database;
		graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
		this.cls = cls;
		views = new ActionViews<E>(graph, cls);
		querier = new Query<E>(graph, cls);

	}

	/**
	 * List all instances of the 'entity' accessible to the given user.
	 * 
	 * @return
	 */
	public StreamingOutput list() {
		try {

			final ObjectMapper mapper = new ObjectMapper();
			final JsonFactory f = new JsonFactory();
			final Iterable<E> list = querier
					.list((Long) getRequesterUserProfileId());

			// FIXME: I don't understand this streaming output system well
			// enough
			// to determine whether this actually streams or not. It certainly
			// doesn't look like it.
			return new StreamingOutput() {
				@Override
				public void write(OutputStream arg0) throws IOException,
						WebApplicationException {
					JsonGenerator g = f.createJsonGenerator(arg0);
					g.writeStartArray();
					for (E item : list) {
						try {
							mapper.writeValue(g,
									converter.vertexFrameToData(item));
						} catch (SerializationError e) {
							throw new RuntimeException(e);
						}
					}
					g.writeEndArray();
					g.close();
				}
			};
		} catch (IndexNotFoundException e) {
			return streamingException(e);
		} catch (PermissionDenied e) {
			return streamingException(e);
		}
	}

	/**
	 * Create an instance of the 'entity' in the database
	 * 
	 * @param json
	 *            The json representation of the entity to create (no vertex
	 *            'id' fields)
	 * @return The response of the create request, the 'location' will contain
	 *         the url of the newly created instance.
	 */
	public Response create(String json) {

		try {
			EntityBundle<VertexFrame> entityBundle = converter
					.jsonToBundle(json);
			E entity = views.create(converter.bundleToData(entityBundle),
					getRequesterUserProfileId());
			String jsonStr = converter.vertexFrameToJson(entity);
			UriBuilder ub = uriInfo.getAbsolutePathBuilder();
			URI docUri = ub.path(entity.asVertex().getId().toString()).build();

			return Response.status(Status.CREATED).location(docUri)
					.entity((jsonStr).getBytes()).build();

		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (DeserializationError e) {
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Retieve (get) an instance of the 'entity' in the database
	 * 
	 * @param id
	 *            The vertex id
	 * @return The response of the request, which contains the json
	 *         representation
	 */
	public Response retrieve(long id) {
		try {
			E entity = views.detail(id, getRequesterUserProfileId());
			String jsonStr = new Converter().vertexFrameToJson(entity);

			return Response.status(Status.OK).entity((jsonStr).getBytes())
					.build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED).build();
		} catch (SerializationError e) {
			// Most likely there was no such item (wrong id)
			// BETTER get a different Exception for that?
			//
			// so we would need to return a BADREQUEST, or NOTFOUND

			return Response.status(Status.NOT_FOUND)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Retieve (get) an instance of the 'entity' in the database
	 * 
	 * @param id
	 *            The Entities identifier string
	 * @return The response of the request, which contains the json
	 *         representation
	 */
	public Response retrieve(String id) {
		return retrieve(AccessibleEntity.IDENTIFIER_KEY, id);
	}

	/**
	 * Retieve (get) an instance of the 'entity' in the database
	 * 
	 * @param key
	 *            The key to search
	 * @param value
	 *            The key's value
	 * @return The response of the request, which contains the json
	 *         representation
	 */
	public Response retrieve(String key, String value) {
		try {
			E entity = querier.get(key, value,
					(long) getRequesterUserProfileId());
			String jsonStr = new Converter().vertexFrameToJson(entity);

			return Response.status(Status.OK).entity((jsonStr).getBytes())
					.build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ItemNotFound e) {
			// Most likely there was no such item (wrong id)
			// BETTER get a different Exception for that?
			//
			// so we would need to return a BADREQUEST, or NOTFOUND

			return Response.status(Status.NOT_FOUND)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (IndexNotFoundException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (SerializationError e) {
			// Just fess-up to this error, since if it happens it'll be
			// our own fault.
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Update (change) an instance of the 'entity' in the database
	 * 
	 * @param json
	 *            The json
	 * @return The response of the update request
	 */
	public Response update(String json) {

		try {
			try {
				EntityBundle<VertexFrame> entityBundle = converter
						.jsonToBundle(json);
				E update = views.update(converter.bundleToData(entityBundle),
						getRequesterUserProfileId());
				String jsonStr = new Converter().vertexFrameToJson(update);

				return Response.status(Status.OK).entity((jsonStr).getBytes())
						.build();
			} catch (DeserializationError e1) {
				return Response.status(Status.BAD_REQUEST)
						.entity(produceErrorMessageJson(e1).getBytes()).build();
			} catch (SerializationError e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity((produceErrorMessageJson(e)).getBytes())
						.build();
			}
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Delete (remove) an instance of the 'entity' in the database
	 * 
	 * @param id
	 *            The vertex id
	 * @return The response of the delete request
	 */
	protected Response delete(long id) {
		try {
			views.delete(id, getRequesterUserProfileId());
			return Response.status(Status.OK).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Delete (remove) an instance of the 'entity' in the database
	 * 
	 * @param id
	 *            The vertex id
	 * @return The response of the delete request
	 */
	protected Response delete(String id) {
		E entity;
		try {
			entity = querier.get(AccessibleEntity.IDENTIFIER_KEY, id,
					getRequesterUserProfileId());
		} catch (ItemNotFound e) {
			return Response.status(Status.NOT_FOUND)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (IndexNotFoundException e) {
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
		return delete((Long) entity.asVertex().getId());
	}

	/*** helpers ***/

	/**
	 * Retrieve the id of the UserProfile of the requester
	 * 
	 * @return The vertex id
	 * @throws PermissionDenied
	 */
	protected Long getRequesterUserProfileId() throws PermissionDenied {
		List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);
		if (list == null || list.isEmpty()) {
			return null;
		} else {
			// just take the first one and get the Long value
			try {
				return Long.parseLong(list.get(0));
			} catch (NumberFormatException e) {
				throw new PermissionDenied("Authorization id has wrong format");
			}
		}
	}
}
