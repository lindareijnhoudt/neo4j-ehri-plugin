package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 * User: michaelb
 *
 * Frame class representing a country. It's identifier should
 * be represented by an ISO3166 Alpha 2 code, lower cased.
 */
@EntityType(EntityClass.COUNTRY)
public interface Country extends IdentifiableEntity, AccessibleEntity, PermissionScope {

    public static final String COUNTRY_CODE = Ontology.IDENTIFIER_KEY;

    /**
     * Alias function for fetching the country code identifier.
     * @return
     */
    @Property(COUNTRY_CODE)
    public String getCode();

    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN)
    public Iterable<Repository> getRepositories();
}
