package eu.ehri.project.persistance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.*;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.utils.ClassUtils;
import org.w3c.dom.Document;

/**
 * Class that represents a graph entity and subtree relations.
 *
 * @author michaelb
 *
 */
public final class Bundle {
    private final String id;
    private final EntityClass type;
    private final ImmutableMap<String, Object> data;
    private final ImmutableMap<String, Object> meta;
    private final ImmutableListMultimap<String, Bundle> relations;

    /**
     * Serialization constant definitions
     */
    public static final String ID_KEY = "id";
    public static final String REL_KEY = "relationships";
    public static final String DATA_KEY = "data";
    public static final String TYPE_KEY = "type";
    public static final String META_KEY = "meta";

    /**
     * Properties that are "managed", i.e. automatically set
     * date/time strings or cache values should begin with a
     * prefix and are ignored Bundle equality calculations.
     */
    public static final String MANAGED_PREFIX = "_";

    /**
     * Constructor.
     *
     * @param id
     * @param type
     * @param data
     * @param relations
     */
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations, final Map<String, Object> meta) {
        this.id = id;
        this.type = type;
        this.data = filterData(data);
        this.meta = ImmutableMap.copyOf(meta);
        this.relations = ImmutableListMultimap.copyOf(relations);
    }

    /**
     * Constructor for bundle without existing id.
     *
     * @param id
     * @param type
     * @param data
     * @param relations
     */
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
        this(id, type, data, relations, Maps.<String,Object>newHashMap());
    }

    /**
     * Constructor for bundle without existing id.
     *
     * @param type
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
        this(null, type, data, relations, Maps.<String,Object>newHashMap());
    }

    /**
     * Constructor for just a type.
     *
     * @param type
     */
    public Bundle(EntityClass type) {
        this(null, type, Maps.<String, Object> newHashMap(), LinkedListMultimap
                .<String, Bundle> create(), Maps.<String,Object>newHashMap());
    }

    /**
     * Constructor for bundle without existing id or relations.
     *
     * @param type
     * @param data
     */
    public Bundle(EntityClass type, final Map<String, Object> data) {
        this(null, type, data, LinkedListMultimap.<String, Bundle> create(),
                Maps.<String,Object>newHashMap());
    }

    /**
     * Get the id of the bundle's graph vertex (or null if it does not yet
     * exist.
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Get a bundle with the given id.
     *
     * @param id
     */
    public Bundle withId(String id) {
        checkNotNull(id);
        return new Bundle(id, type, data, relations, meta);
    }

    /**
     * Get the type of entity this bundle represents as per the target class's
     * entity type key.
     *
     * @return
     */
    public EntityClass getType() {
        return type;
    }

    /**
     * Get a data value.
     *
     * @return
     */
    public Object getDataValue(String key) {
        checkNotNull(key);
        return data.get(key);
    }

    /**
     * Set a value in the bundle's data.
     *
     * @param key
     * @param value
     * @return
     */
    public Bundle withDataValue(String key, Object value) {
        if (value == null) {
            return this;
        } else {
            Map<String, Object> newData = Maps.newHashMap(data);
            newData.put(key, value);
            return withData(newData);
        }
    }

    /**
     * Remove a value in the bundle's data.
     *
     * @param key
     * @return
     */
    public Bundle removeDataValue(String key) {
        Map<String, Object> newData = Maps.newHashMap(data);
        newData.remove(key);
        return withData(newData);
    }

    /**
     * Get the bundle data.
     *
     * @return
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Get the bundle metadata
     */
    public Map<String,Object> getMeta() {
        return meta;
    }


    /**
     * Set the entire data map for this bundle.
     *
     * @param data
     * @return
     */
    public Bundle withData(final Map<String, Object> data) {
        return new Bundle(id, type, data, relations, meta);
    }

    /**
     * Get the bundle's relation bundles.
     *
     * @return
     */
    public ListMultimap<String, Bundle> getRelations() {
        return relations;
    }

    /**
     * Set entire set of relations.
     *
     * @param relations
     * @return
     */
    public Bundle withRelations(ListMultimap<String, Bundle> relations) {
        return new Bundle(id, type, data, relations, meta);
    }

    /**
     * Get a set of relations.
     *
     * @param relation
     * @return
     */
    public List<Bundle> getRelations(String relation) {
        return relations.get(relation);
    }

    /**
     * Set bundles for a particular relation.
     *
     * @param relation
     * @param others
     * @return
     */
    public Bundle withRelations(String relation, List<Bundle> others) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
                .create(relations);
        tmp.putAll(relation, others);
        return new Bundle(id, type, data, tmp, meta);
    }

    /**
     * Add a bundle for a particular relation.
     *
     * @param relation
     * @param other
     */
    public Bundle withRelation(String relation, Bundle other) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
                .create(relations);
        tmp.put(relation, other);
        return new Bundle(id, type, data, tmp, meta);
    }

    /**
     * Check if this bundle contains the given relation set.
     *
     * @param relation
     * @return
     */
    public boolean hasRelations(String relation) {
        return relations.containsKey(relation);
    }

    /**
     * Remove a single relation.
     *
     * @param relation
     * @return
     */
    public Bundle removeRelation(String relation, Bundle item) {
        ListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.remove(relation, item);
        return new Bundle(id, type, data, tmp, meta);
    }

    /**
     * Remove a set of relationships.
     *
     * @param relation
     * @return
     */
    public Bundle removeRelations(String relation) {
        ListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.removeAll(relation);
        return new Bundle(id, type, data, tmp, meta);
    }

    /**
     * Get the target class.
     *
     * @return
     */
    public Class<?> getBundleClass() {
        return type.getEntityClass();
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     *
     * @return
     */
    public Iterable<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(type.getEntityClass());
    }

    /**
     * Return a list of property keys which must be unique.
     *
     * @return
     */
    public Iterable<String> getUniquePropertyKeys() {
        return ClassUtils.getUniquePropertyKeys(type.getEntityClass());
    }

    /**
     * Create a bundle from raw data.
     *
     * @param data
     * @return
     * @throws DeserializationError
     */
    public static Bundle fromData(Object data) throws DeserializationError {
        return DataConverter.dataToBundle(data);
    }

    /**
     * Serialize a bundle to raw data.
     *
     * @return
     */
    public Map<String, Object> toData() {
        return DataConverter.bundleToData(this);
    }

    /**
     * Create a bundle from a (JSON) string.
     *
     * @param json
     * @return
     * @throws DeserializationError
     */
    public static Bundle fromString(String json) throws DeserializationError {
        return DataConverter.jsonToBundle(json);
    }

    @Override
    public String toString() {
        return "<" + getType() + "> (" + getData() + " + Rels: " + relations + ")";
    }

    /**
     * Serialize a bundle to a JSON string.
     * @return json string
     */
    public String toJson() {
        try {
            return DataConverter.bundleToJson(this);
        } catch (SerializationError e) {
            return "Invalid Bundle: " + e.getMessage();
        }
    }

    /**
     * Serialize a bundle to a JSON string.
     * @return document
     */
    public Document toXml() {
        return DataConverter.bundleToXml(this);
    }

    /**
     * Serialize to an XML String.
     * @return
     */
    public String toXmlString() {
        return DataConverter.bundleToXmlString(this);
    }

    /**
     * Return an immutable copy of the given data map with nulls removed.
     * @param data
     * @return
     */
    private ImmutableMap<String, Object> filterData(Map<String, Object> data) {
        Map<String,Object> filtered = Maps.newHashMap();
        for (Map.Entry<? extends String,Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return ImmutableMap.copyOf(filtered);
    }

    private Map<String,Object> unmanagedData(Map<String, Object> in) {
        Map<String,Object> filtered = Maps.newHashMap();
        for (Map.Entry<? extends String,Object> entry : in.entrySet()) {
            if (!entry.getKey().startsWith(MANAGED_PREFIX)
                    && entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bundle bundle = (Bundle) o;

        if (type != bundle.type) return false;
        if (!unmanagedData(data).equals(unmanagedData(bundle.data))) return false;
        if (!unorderedRelations(relations)
                .equals(unorderedRelations(bundle.relations))) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + unmanagedData(data).hashCode();
        result = 31 * result + unorderedRelations(relations).hashCode();
        return result;
    }

    /**
     * Convert the ordered relationship set into an unordered one for comparison.
     * FIXME: Clean up the code and optimise this function.
     * @param rels
     * @return
     */
    private Map<String,LinkedHashMultiset<Bundle>> unorderedRelations(final ListMultimap<String,Bundle> rels) {
        Map<String,LinkedHashMultiset<Bundle>> map = Maps.newHashMap();
        for (Map.Entry<String,Collection<Bundle>> entry : rels.asMap().entrySet()) {
            map.put(entry.getKey(), LinkedHashMultiset.create(entry.getValue()));
        }
        return map;
    }
}
