/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.Bundle;
import java.util.HashMap;
import java.util.Map;

import eu.ehri.project.persistance.BundleValidator;
import eu.ehri.project.persistance.BundleValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class UkrainianUnitImporter extends XmlImporter<Object> {

    private XmlImportProperties p;
    private static final Logger logger = LoggerFactory.getLogger(UkrainianUnitImporter.class);

    public UkrainianUnitImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        p = new XmlImportProperties("ukraine.properties");
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        logger.debug("-----------------------------------");
        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractUnit(itemData));
        Map<String, Object> unknowns = extractUnknownProperties(itemData);

        String lang = itemData.get("language_of_description").toString();
        if (lang.indexOf(", ") > 0) {
            String[] langs = lang.split(", ");
            for (String l : langs) {
                Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, l));
                descBundle = descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, constructDateMap(itemData)));
                if (!unknowns.isEmpty()) {
                    descBundle = descBundle.withRelation(Description.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
                }
                unit = unit.withRelation(Description.DESCRIBES, descBundle);
            }
        } else {
            Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, lang));
            descBundle = descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, constructDateMap(itemData)));
            if (!unknowns.isEmpty()) {
                descBundle = descBundle.withRelation(Description.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
            }

            unit = unit.withRelation(Description.DESCRIBES, descBundle);
        }

        BundleValidator validator = BundleValidatorFactory.getInstance(manager, unit);
        validator.validateTree();

        String id = unit.getType().getIdgen().generateId(EntityClass.DOCUMENTARY_UNIT, permissionScope, unit);
        boolean exists = manager.exists(id);
        DocumentaryUnit frame = persister.createOrUpdate(unit.withId(id), DocumentaryUnit.class);
        if (!permissionScope.equals(SystemScope.getInstance())) {
            frame.setRepository(framedGraph.frame(permissionScope.asVertex(), Repository.class));
            frame.setPermissionScope(permissionScope);
        }

        if (exists) {
            for (ImportCallback cb : updateCallbacks) {
                cb.itemImported(frame);
            }
        } else {
            for (ImportCallback cb : createCallbacks) {
                cb.itemImported(frame);
            }
        }
        return frame;

    }

    private Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (p.getProperty(key).equals("UNKNOWN")) {
                unknowns.put(key, itemData.get(key));
            }
        }
        return unknowns;
    }


    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = new HashMap<String, Object>();
        if (itemData.containsKey("identifier")) {
            item.put(IdentifiableEntity.IDENTIFIER_KEY, itemData.get("identifier"));
        } else {
            logger.error("missing identifier");
        }
        return item;
    }

    public Map<String, Object> constructDateMap(Map<String, Object> itemData) {
        Map<String, Object> item = new HashMap<String, Object>();
        String origDate = itemData.get("dates").toString();
        if (origDate.indexOf(",,") > 0) {
            String[] dates = itemData.get("dates").toString().split(",,");
            item.put(DatePeriod.START_DATE, dates[0]);
            item.put(DatePeriod.END_DATE, dates[1]);
        } else {
            item.put(DatePeriod.START_DATE, origDate);
            item.put(DatePeriod.END_DATE, origDate);
        }
        return item;
    }

    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, String language) {
        Map<String, Object> item = new HashMap<String, Object>();


        for (String key : itemData.keySet()) {
            if ((!key.equals("identifier")) && 
                    !(p.getProperty(key).equals("IGNORE")) && 
                    !(p.getProperty(key).equals("UNKNOWN")) && 
                    (!key.equals("dates")) && 
                    (!key.equals("language_of_description"))
                    ) {
                if (!p.containsProperty(key)) {
                    SaxXmlHandler.putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    SaxXmlHandler.putPropertyInGraph(item, p.getProperty(key), itemData.get(key).toString());
                }
            }

        }
        //replace the language from the itemData with the one specified in the param
        SaxXmlHandler.putPropertyInGraph(item, Description.LANGUAGE_CODE, language);
        return item;
    }

    @Override
    public Iterable<Map<String, Object>> extractDates(Object data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}