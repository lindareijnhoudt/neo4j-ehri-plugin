package eu.ehri.extension;

import static eu.ehri.extension.RestHelpers.produceErrorMessageJson;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.Converter;

/**
 * Provides a RESTfull interface for the Action class. Note: Action instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path("admin")
public class AdminResource {

    public static String DEFAULT_USER_ID_PREFIX = "user";
    public static String DEFAULT_USER_ID_FORMAT = "%s%06d";

    private GraphDatabaseService database;
    private FramedGraph<Neo4jGraph> graph;
    private Converter converter;

    public AdminResource(@Context GraphDatabaseService database) {
        this.database = database;
        this.graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
        converter = new Converter();
    }

    /**
     * Create a new user with a default name and identifier.
     * 
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/createDefaultUserProfile")
    public Response createDefaultUserProfile() {
        Transaction tx = database.beginTx();
        try {
            String ident = getNextDefaultUserName();
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(AccessibleEntity.IDENTIFIER_KEY, ident);
            data.put(Accessor.NAME, ident);

            // TODO: Create an action for this with the system user...
            BundleDAO<UserProfile> persister = new BundleDAO<UserProfile>(graph);
            UserProfile user = persister.create(new BundleFactory<UserProfile>()
                    .buildBundle(data, UserProfile.class));
            String jsonStr = converter.vertexFrameToJson(user);
            return Response.status(Status.CREATED)
                    .entity((jsonStr).getBytes()).build();

        } catch (Exception e) {
            tx.failure();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build(); 
        } finally {
            tx.success();
        }
    }

    // Helpers...
    
    private String getNextDefaultUserName() {
        Index<Vertex> index = graph.getBaseGraph().getIndex(
                EntityTypes.USER_PROFILE, Vertex.class);
        
        // FIXME: It's crappy to have to iterate all the items to count them...
        long userCount = 0;
        CloseableIterable<Vertex> query = index.query(AccessibleEntity.IDENTIFIER_KEY, "*");
        try {
            for (@SuppressWarnings("unused") Vertex _ : query) userCount++;
        } finally {
            query.close();
        }
        long start = userCount + 1;
        while (index.count(
                AccessibleEntity.IDENTIFIER_KEY, String.format(
                        DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX,
                        start)) > 0) start++;
        return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX,
                start);
    }
}
