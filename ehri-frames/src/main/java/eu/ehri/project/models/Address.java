package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.VersionedEntity;

@EntityType(EntityClass.ADDRESS)
public interface Address extends VersionedEntity {

    @Adjacency(label = AgentDescription.HAS_ADDRESS, direction = Direction.IN)
    public AgentDescription getAgentDescription();
}
