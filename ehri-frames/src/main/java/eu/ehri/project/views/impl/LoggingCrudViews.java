package eu.ehri.project.views.impl;

import com.google.common.base.Optional;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.*;

import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.MutationState;
import eu.ehri.project.views.Crud;
import eu.ehri.project.persistance.Mutation;

/**
 * Views class that handles creating Action objects that provide an audit log
 * for CRUD actions.
 * 
 * @author michaelb
 * 
 * @param <E>
 */
public class LoggingCrudViews<E extends AccessibleEntity> implements Crud<E> {

    private final ActionManager actionManager;
    private final CrudViews<E> views;
    private final FramedGraph<?> graph;
    private final Class<E> cls;
    @SuppressWarnings("unused")
    private final PermissionScope scope;

    /**
     * Scoped Constructor.
     * 
     * @param graph
     * @param cls
     */
    public LoggingCrudViews(FramedGraph<?> graph, Class<E> cls,
            PermissionScope scope) {
        this.graph = graph;
        this.cls = cls;
        this.scope = Optional.fromNullable(scope).or(SystemScope.getInstance());
        actionManager = new ActionManager(graph, this.scope);
        views = new CrudViews<E>(graph, cls, this.scope);
    }

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public LoggingCrudViews(FramedGraph<?> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Create a new object of type `E` from the given data, saving an Action log
     * with the default creation message.
     * 
     * @param bundle
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public E create(Bundle bundle, Accessor user) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return create(bundle, user, Optional.<String>absent());
    }

    /**
     * Create a new object of type `E` from the given data, saving an Action log
     * with the given log message.
     * 
     * @param bundle
     * @param user
     * @param logMessage
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public E create(Bundle bundle, Accessor user, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        // Behold: A compelling reason to upgrade to Java 7
        // http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html
        E out = views.create(bundle, user);
        actionManager.logEvent(out, graph.frame(user.asVertex(), Actioner.class),
                EventTypes.creation, logMessage);
        return out;
    }

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the default creation message.
     * 
     * @param bundle
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public Mutation<E> createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        return createOrUpdate(bundle, user, Optional.<String>absent());
    }

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the given log message.
     * 
     * @param bundle
     * @param user
     * @param logMessage
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public Mutation<E> createOrUpdate(Bundle bundle, Accessor user, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Mutation<E> out = views.createOrUpdate(bundle, user);
        if (out.getState() == MutationState.UPDATED) {
            actionManager.logEvent(out.getNode(), graph.frame(user.asVertex(), Actioner.class),
                    EventTypes.modification, logMessage);
        }
        return out;
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the default update message.
     * 
     * @param bundle
     * @param user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public Mutation<E> update(Bundle bundle, Accessor user) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return update(bundle, user, Optional.<String>absent());
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the given log message.
     * 
     * @param bundle
     * @param user
     * @param logMessage
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public Mutation<E> update(Bundle bundle, Accessor user, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        try {
            Mutation<E> out = views.update(bundle, user);
            if (out.getState() != MutationState.UNCHANGED) {
                actionManager.logEvent(out.getNode(), graph.frame(user.asVertex(), Actioner.class),
                        EventTypes.modification, logMessage);
            }
            return out;
        } catch (ItemNotFound ex) {
            throw new RuntimeException(ex); // FIXME: Remove this...
        }
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the default update message.
     *
     * @param bundle
     * @param parent
     * @param user
     * @param dependentClass
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public <T extends Frame> Mutation<T> updateDependent(Bundle bundle, E parent, Accessor user,
            Class<T> dependentClass) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return updateDependent(bundle, parent, user, dependentClass, Optional.<String>absent());
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the given log message.
     *
     * @param bundle
     * @param parent
     * @param user
     * @param dependentClass
     * @param logMessage
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public <T extends Frame> Mutation<T> updateDependent(Bundle bundle, E parent, Accessor user,
            Class<T> dependentClass, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        try {
            Mutation<T> out = views.updateDependent(bundle, parent, user, dependentClass);
            if (out.getState() != MutationState.UNCHANGED) {
                actionManager.setScope(parent).logEvent(graph.frame(user.asVertex(),
                        Actioner.class), EventTypes.modification, logMessage);
            }
            return out;
        } catch (ItemNotFound ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the default update message.
     *
     * @param bundle
     * @param parent
     * @param user
     * @param dependentClass
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public <T extends Frame> T createDependent(Bundle bundle,
            E parent, Accessor user, Class<T> dependentClass) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return createDependent(bundle, parent, user, dependentClass, Optional.<String>absent());
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the given log message.
     *
     * @param bundle
     * @param parent
     * @param user
     * @param dependentClass
     * @param logMessage
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public <T extends Frame> T createDependent(Bundle bundle, E parent, Accessor user,
                Class<T> dependentClass, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        T out = views.createDependent(bundle, parent, user, dependentClass);
        actionManager.setScope(parent).logEvent(
                graph.frame(user.asVertex(), Actioner.class), EventTypes.creation, logMessage);
        return out;
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the default deletion message.
     * 
     * @param item
     * @param user
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError {
        return delete(item, user, Optional.<String>absent());
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the given deletion message.
     * 
     * @param item
     * @param user
     * @param logMessage
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, SerializationError {
        actionManager.logEvent(item, graph.frame(user.asVertex(), Actioner.class),
                EventTypes.deletion, logMessage);
        return views.delete(item, user);
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the default deletion message.
     *
     * @param item
     * @param parent
     * @param user
     * @param dependentClass
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public <T extends Frame> Integer deleteDependent(T item, E parent, Accessor user,
            Class<T> dependentClass) throws PermissionDenied,
            ValidationError, SerializationError {
        return deleteDependent(item, parent, user, dependentClass, Optional.<String>absent());
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the given deletion message.
     *
     * @param item
     * @param parent
     * @param user
     * @param dependentClass
     * @param logMessage
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public <T extends Frame> Integer deleteDependent(T item, E parent, Accessor user,
            Class<T> dependentClass, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, SerializationError {
        actionManager.setScope(parent).logEvent(graph.frame(user.asVertex(), Actioner.class),
                EventTypes.deletion, logMessage);
        return views.deleteDependent(item, parent, user, dependentClass);
    }


    public LoggingCrudViews<E> setScope(PermissionScope scope) {
        return new LoggingCrudViews<E>(graph, cls,
                Optional.fromNullable(scope).or(SystemScope.INSTANCE));
    }

    public E detail(E item, Accessor user) throws AccessDenied {
        return views.detail(item, user);
    }
}
