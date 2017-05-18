/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.metamodel.EntityType;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.OptimisticCacheSource;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.loader.spi.MultiLoadOptions;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.queryable.spi.NavigableReferenceInfo;
import org.hibernate.metamodel.queryable.spi.RootTableGroupContext;
import org.hibernate.metamodel.queryable.spi.RootTableGroupProducer;
import org.hibernate.metamodel.queryable.spi.SqlAliasBaseResolver;
import org.hibernate.metamodel.queryable.spi.TableGroupContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeFactory;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeClassResolver;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.spi.Type;

/**
 * Contract describing mapping information and persistence logic for a particular strategy of entity mapping.  A given
 * persister instance corresponds to a given mapped entity class.
 * <p/>
 * Implementations must be thread-safe (preferably immutable).
 * <p/>
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see RuntimeModelNodeFactory
 * @see RuntimeModelNodeClassResolver
 * @see #STANDARD_CONSTRUCTOR_SIG
 *
 * @since 6.0
 */
@Incubating
public interface EntityTypeImplementor<T>
		extends NavigableEntityValued<T>, NavigableContainer<T>, EmbeddedContainer<T>,
				RootTableGroupProducer, OptimisticCacheSource, IdentifiableTypeImplementor<T>, EntityType<T> {

	// todo (6.0) : ? - can OptimisticCacheSource stuff go away?
	// 		It was added for caches like JBoss Tree Cache that supported MVCC-based
	//		transactional consistency via integration with JTA.  Do not believe
	//		any of our existing cache integrations support MVCC.


	/**
	 * Unless a custom {@link RuntimeModelNodeFactory} is used, it is expected
	 * that implementations of EntityPersister define a constructor accepting the following arguments:<ol>
	 *     <li>
	 *         {@link org.hibernate.mapping.PersistentClass} - describes the metadata about the entity
	 *         to be handled by the persister
	 *     </li>
	 *     <li>
	 *         {@link EntityRegionAccessStrategy} - the second level caching strategy for this entity
	 *     </li>
	 *     <li>
	 *         {@link NaturalIdRegionAccessStrategy} - the second level caching strategy for the natural-id
	 *         defined for this entity, if one
	 *     </li>
	 *     <li>
	 *         {@link RuntimeModelCreationContext} - access to additional
	 *         information useful while constructing the persister.
	 *     </li>
	 * </ol>
	 */
	Class[] STANDARD_CONSTRUCTOR_SIG = new Class[] {
			PersistentClass.class,
			EntityRegionAccessStrategy.class,
			NaturalIdRegionAccessStrategy.class,
			RuntimeModelCreationContext.class
	};


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Redesigned contract

	/**
	 * Called after all EntityPersister instance for the persistence unit have been created;
	 *
	 * @param superType The entity's super's EntityPersister
	 * @param mappingDescriptor Should be  the same reference (instance) originally passed to the
	 * 		ctor, but we want to not have to store that reference as instance state -
	 * 		so we pass it in again
	 * @param creationContext Access to the database model, etc
	 */
	void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			EntityMapping mappingDescriptor,
			RuntimeModelCreationContext creationContext);

	/**
	 * Called after {@link #finishInitialization} has been called on all persisters.
	 */
	void postInstantiate();

	/**
	 * The entity name which this persister maps.
	 */
	String getEntityName();

	EntityJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Return the SessionFactory to which this persister "belongs".
	 */
	SessionFactoryImplementor getFactory();

	/**
	 * Access to information about the entity's inheritance hierarchy.
	 *
	 * @since 6.0
	 */
	EntityHierarchy getHierarchy();

	/**
	 * Access to information about the entity identifier, specifically relative to this
	 * entity (in terms of Java parameterized type signature in regards to all "id attributes").
	 * Generally this delegates to {@link EntityHierarchy#getIdentifierDescriptor()} via
	 * {@link #getHierarchy()}.  We'd want to override the value coming from
	 * {@link EntityHierarchy#getIdentifierDescriptor()} in cases where we have a
	 * MappedSuperclass to the root entity and that MappedSuperclass defines the identifier
	 * (or any attributes really) using a parameterized type signature where the attribute
	 * type has not been concretely bound and is instead bound on the root entity.
	 *
	 * @todo (6.0) : we should consider doing the same for normal attributes (and version?) as well
	 * 		in terms of cases where generic type parameters for an entity hierarchy have not
	 * 		been bound as of the root entity
	 *
	 * 	@todo (6.0) : how should we handle the attribute as defined on the MappedSuperclass and the attribute defined on the subclass (with the conretely bound parameter type)?
	 * 		I mean specifically.. do we mark the MappedSuperclass attributes (somehow) as having an "unbound" attribute type and
	 * 		mark the subclass attribute as being a "bridged" attribute?
	 *
	 * @since 6.0
	 */
	EntityIdentifier getIdentifierDescriptor();

	/**
	 * Get the EntityEntryFactory indicated for the entity mapped by this persister.
	 */
	EntityEntryFactory getEntityEntryFactory();

	/**
	 * Access to information about bytecode enhancement for this entity.
	 */
	BytecodeEnhancementMetadata getBytecodeEnhancementMetadata();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// loader/locker support

	/**
	 * @todo (6.0) what args?
	 */
	SingleIdEntityLoader getSingleIdLoader(LockOptions lockOptions, SharedSessionContractImplementor session);

	/**
	 * @todo (6.0) what args?
	 */
	MultiIdEntityLoader getMultiIdLoader(SharedSessionContractImplementor session);

	/**
	 * @todo (6.0) what args?
	 */
	SingleUniqueKeyEntityLoader getSingleUniqueKeyLoader(Navigable navigable, SharedSessionContractImplementor session);

	/**
	 * @todo (6.0) - other args?
	 */
	EntityLocker getLocker(LockOptions lockOptions, SharedSessionContractImplementor session);

	/**
	 * Apply the Tables mapped by this entity to the collector as TableReferences; return
	 * the root table for the added joined tables for the caller to use as a join target.
	 */
	void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBaseManager.SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// `org.hibernate.metamodel.queryable` support


	@Override
	EntityTableGroup createRootTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			RootTableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver);

	EntityTableGroup createEntityTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			TableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver);


	/**
	 * Access to the root table for this entity.
	 */
	Table getRootTable();

	/**
	 * Access to all "declared" secondary table mapping info for this entity.
	 * This does not include secondary tables from super-types.
	 */
	List<JoinedTableBinding> getSecondaryTableBindings();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitEntity( this );
	}





	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy contract (sans methods moved to above "Redesigned contract") section

	/**
	 * The property name of the "special" identifier property in HQL
	 */
	String ENTITY_ID = "id";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns an object that identifies the space in which identifiers of
	 * this entity hierarchy are unique.  Might be a table name, a JNDI URL, etc.
	 *
	 * @return The root entity name.
	 */
	String getRootEntityName();

	/**
	 * Retrieve the underlying entity metamodel instance...
	 *
	 *@return The metamodel
	 */
	EntityMetamodel getEntityMetamodel();

	/**
	 * Determine whether the given name represents a subclass entity
	 * (or this entity itself) of the entity mapped by this persister.
	 *
	 * @param entityName The entity name to be checked.
	 * @return True if the given entity name represents either the entity
	 * mapped by this persister or one of its subclass entities; false
	 * otherwise.
	 */
	boolean isSubclassEntityName(String entityName);

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class only.
	 * <p/>
	 * For most implementations, this returns the complete set of table names
	 * to which instances of the mapped entity are persisted (not accounting
	 * for superclass entity mappings).
	 *
	 * @return The property spaces.
	 */
	Serializable[] getPropertySpaces();

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class and its subclasses.
	 * <p/>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The query spaces.
	 */
	Serializable[] getQuerySpaces();

	/**
	 * Determine whether this entity supports dynamic proxies.
	 *
	 * @return True if the entity has dynamic proxy support; false otherwise.
	 */
	boolean hasProxy();

	/**
	 * Determine whether this entity contains references to persistent collections.
	 *
	 * @return True if the entity does contain persistent collections; false otherwise.
	 */
	boolean hasCollections();

	/**
	 * Determine whether any properties of this entity are considered mutable.
	 *
	 * @return True if any properties of the entity are mutable; false otherwise (meaning none are).
	 */
	boolean hasMutableProperties();

	/**
	 * Determine whether this entity contains references to persistent collections
	 * which are fetchable by subselect?
	 *
	 * @return True if the entity contains collections fetchable by subselect; false otherwise.
	 */
	boolean hasSubselectLoadableCollections();

	/**
	 * Determine whether this entity has any non-none cascading.
	 *
	 * @return True if the entity has any properties with a cascade other than NONE;
	 * false otherwise (aka, no cascading).
	 */
	boolean hasCascades();

	/**
	 * Determine whether instances of this entity are considered mutable.
	 *
	 * @return True if the entity is considered mutable; false otherwise.
	 */
	boolean isMutable();

	/**
	 * Determine whether the entity is inherited one or more other entities.
	 * In other words, is this entity a subclass of other entities.
	 *
	 * @return True if other entities extend this entity; false otherwise.
	 */
	boolean isInherited();

	/**
	 * Are identifiers of this entity assigned known beforeQuery the insert execution?
	 * Or, are they generated (in the database) by the insert execution.
	 *
	 * @return True if identifiers for this entity are generated by the insert
	 * execution.
	 */
	boolean isIdentifierAssignedByInsert();

	/**
	 * Get the type of a particular property by name.
	 *
	 * @param propertyName The name of the property for which to retrieve
	 * the type.
	 * @return The type.
	 * @throws org.hibernate.MappingException Typically indicates an unknown
	 * property name.
	 */
	Type getPropertyType(String propertyName) throws MappingException;

	/**
	 * Compare the two snapshots to determine if they represent dirty state.
	 *
	 * @param currentState The current snapshot
	 * @param previousState The baseline snapshot
	 * @param owner The entity containing the state
	 * @param session The originating session
	 * @return The indices of all dirty properties, or null if no properties
	 * were dirty.
	 */
	int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session);

	/**
	 * Compare the two snapshots to determine if they represent modified state.
	 *
	 * @param old The baseline snapshot
	 * @param current The current snapshot
	 * @param object The entity containing the state
	 * @param session The originating session
	 * @return The indices of all modified properties, or null if no properties
	 * were modified.
	 */
	int[] findModified(Object[] old, Object[] current, Object object, SharedSessionContractImplementor session);

	/**
	 * Determine whether the entity has a particular property holding
	 * the identifier value.
	 *
	 * @return True if the entity has a specific property holding identifier value.
	 */
	boolean hasIdentifierProperty();

	/**
	 * Determine whether detached instances of this entity carry their own
	 * identifier value.
	 * <p/>
	 * The other option is the deprecated feature where users could supply
	 * the id during session calls.
	 *
	 * @return True if either (1) {@link #hasIdentifierProperty()} or
	 * (2) the identifier is an embedded composite identifier; false otherwise.
	 */
	boolean canExtractIdOutOfEntity();

	/**
	 * Determine whether optimistic locking by column is enabled for this
	 * entity.
	 *
	 * @return True if optimistic locking by column (i.e., <version/> or
	 * <timestamp/>) is enabled; false otherwise.
	 */
	boolean isVersioned();

	/**
	 * If {@link #isVersioned()}, then what is the type of the property
	 * holding the locking value.
	 *
	 * @return The Type of the property; or null, if not versioned.
	 */
	Type getVersionType();

	/**
	 * If {@link #isVersioned()}, then what is the index of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or -66, if not versioned.
	 */
	int getVersionProperty();

	/**
	 * Determine whether this entity defines a natural identifier.
	 *
	 * @return True if the entity defines a natural id; false otherwise.
	 */
	boolean hasNaturalIdentifier();

	/**
	 * If the entity defines a natural id ({@link #hasNaturalIdentifier()}), which
	 * properties make up the natural id.
	 *
	 * @return The indices of the properties making of the natural id; or
	 * null, if no natural id is defined.
	 */
	int[] getNaturalIdentifierProperties();

	/**
	 * Retrieve the current state of the natural-id properties from the database.
	 *
	 * @param id The identifier of the entity for which to retrieve the natural-id values.
	 * @param session The session from which the request originated.
	 * @return The natural-id snapshot.
	 */
	Object[] getNaturalIdentifierSnapshot(Serializable id, SharedSessionContractImplementor session);

	/**
	 * Determine which identifier generation strategy is used for this entity.
	 *
	 * @return The identifier generation strategy.
	 */
	IdentifierGenerator getIdentifierGenerator();

	/**
	 * Determine whether this entity defines any lazy properties (ala
	 * bytecode instrumentation).
	 *
	 * @return True if the entity has properties mapped as lazy; false otherwise.
	 */
	boolean hasLazyProperties();

	/**
	 * Load the id for the entity based on the natural id.
	 */
	Serializable loadEntityIdByNaturalId(
			Object[] naturalIdValues, LockOptions lockOptions,
			SharedSessionContractImplementor session);

	/**
	 * Load an instance of the persistent class.
	 */
	Object load(Serializable id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Load an instance of the persistent class.
	 */
	Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Performs a load of multiple entities (of this type) by identifier simultaneously.
	 *
	 * @param ids The identifiers to load
	 * @param session The originating Sesison
	 * @param loadOptions The options for loading
	 *
	 * @return The loaded, matching entities
	 */
	List multiLoad(Serializable[] ids, SharedSessionContractImplementor session, MultiLoadOptions loadOptions);

	/**
	 * Do a version check (optional operation)
	 */
	void lock(Serializable id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Do a version check (optional operation)
	 */
	void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance
	 */
	void insert(Serializable id, Object[] fields, Object object, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance, using a natively generated identifier (optional operation)
	 */
	Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Delete a persistent instance
	 */
	void delete(Serializable id, Object version, Object object, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Update a persistent instance
	 */
	void update(
			Serializable id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session
	) throws HibernateException;

	/**
	 * Get the Hibernate types of the class properties
	 */
	Type[] getPropertyTypes();

	/**
	 * Get the names of the class properties - doesn't have to be the names of the
	 * actual Java properties (used for XML generation only)
	 */
	String[] getPropertyNames();

	/**
	 * Get the "insertability" of the properties of this class
	 * (does the property appear in an SQL INSERT)
	 */
	boolean[] getPropertyInsertability();

	/**
	 * Which of the properties of this class are database generated values on insert?
	 *
	 * @deprecated Replaced internally with InMemoryValueGenerationStrategy / InDatabaseValueGenerationStrategy
	 */
	@Deprecated
	ValueInclusion[] getPropertyInsertGenerationInclusions();

	/**
	 * Which of the properties of this class are database generated values on update?
	 *
	 * @deprecated Replaced internally with InMemoryValueGenerationStrategy / InDatabaseValueGenerationStrategy
	 */
	@Deprecated
	ValueInclusion[] getPropertyUpdateGenerationInclusions();

	/**
	 * Get the "updateability" of the properties of this class
	 * (does the property appear in an SQL UPDATE)
	 */
	boolean[] getPropertyUpdateability();

	/**
	 * Get the "checkability" of the properties of this class
	 * (is the property dirty checked, does the cache need
	 * to be updated)
	 */
	boolean[] getPropertyCheckability();

	/**
	 * Get the nullability of the properties of this class
	 */
	boolean[] getPropertyNullability();

	/**
	 * Get the "versionability" of the properties of this class
	 * (is the property optimistic-locked)
	 */
	boolean[] getPropertyVersionability();
	boolean[] getPropertyLaziness();
	/**
	 * Get the cascade styles of the properties (optional operation)
	 */
	CascadeStyle[] getPropertyCascadeStyles();

	/**
	 * Get the identifier type
	 */
	Type getIdentifierType();

	/**
	 * Get the name of the identifier property (or return null) - need not return the
	 * name of an actual Java property
	 */
	String getIdentifierPropertyName();

	/**
	 * Should we always invalidate the cache instead of
	 * recaching updated state
	 */
	boolean isCacheInvalidationRequired();
	/**
	 * Should lazy properties of this entity be cached?
	 */
	boolean isLazyPropertiesCacheable();
	/**
	 * Does this class have a cache.
	 */
	boolean hasCache();
	/**
	 * Get the cache (optional operation)
	 */
	EntityRegionAccessStrategy getCacheAccessStrategy();
	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();

	CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session);

	/**
	 * Does this class have a natural id cache
	 */
	boolean hasNaturalIdCache();
	
	/**
	 * Get the NaturalId cache (optional operation)
	 */
	NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy();

	/**
	 * Get the user-visible metadata for the class (optional operation)
	 */
	ClassMetadata getClassMetadata();

	/**
	 * Is batch loading enabled?
	 */
	boolean isBatchLoadable();

	/**
	 * Is select snapshot beforeQuery update enabled?
	 */
	boolean isSelectBeforeUpdateRequired();

	/**
	 * Get the current database state of the object, in a "hydrated" form, without
	 * resolving identifiers
	 * @return null if there is no row in the database
	 */
	Object[] getDatabaseSnapshot(Serializable id, SharedSessionContractImplementor session) throws HibernateException;

	Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SharedSessionContractImplementor session);

	/**
	 * Get the current version of the object, or return null if there is no row for
	 * the given identifier. In the case of unversioned data, return any object
	 * if the row exists.
	 */
	Object getCurrentVersion(Serializable id, SharedSessionContractImplementor session) throws HibernateException;

	Object forceVersionIncrement(Serializable id, Object currentVersion, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Has the class actually been bytecode instrumented?
	 */
	boolean isInstrumented();

	/**
	 * Does this entity define any properties as being database generated on insert?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	boolean hasInsertGeneratedProperties();

	/**
	 * Does this entity define any properties as being database generated on update?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	boolean hasUpdateGeneratedProperties();

	/**
	 * Does this entity contain a version property that is defined
	 * to be database generated?
	 *
	 * @return true if this entity contains a version property and that
	 * property has been marked as generated.
	 */
	boolean isVersionPropertyGenerated();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is tuplizer-centric, but is passed a session ~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Called just after the entities properties have been initialized
	 */
	void afterInitialize(Object entity, SharedSessionContractImplementor session);

	/**
	 * Called just afterQuery the entity has been reassociated with the session
	 */
	void afterReassociate(Object entity, SharedSessionContractImplementor session);

	/**
	 * Create a new proxy instance
	 */
	Object createProxy(Serializable id, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Is this a new transient instance?
	 */
	Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Return the values of the insertable properties of the object (including backrefs)
	 */
	Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p/>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext beforeQuery calling this method.
	 *
	 * @param id The entity's id value.
	 * @param entity The entity for which to get the state.
	 * @param state
	 * @param session The session
	 */
	void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session);
	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p/>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext beforeQuery calling this method.
	 *
	 * @param id The entity's id value.
	 * @param entity The entity for which to get the state.
	 * @param state
	 * @param session The session
	 */
	void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	Class getMappedClass();

	/**
	 * Does the class implement the {@link org.hibernate.classic.Lifecycle} interface.
	 */
	boolean implementsLifecycle();

	/**
	 * Get the proxy interface that instances of <em>this</em> concrete class will be
	 * cast to (optional operation).
	 */
	Class getConcreteProxyClass();

	/**
	 * Set the given values to the mapped properties of the given object
	 */
	void setPropertyValues(Object object, Object[] values);

	/**
	 * Set the value of a particular property
	 */
	void setPropertyValue(Object object, int i, Object value);

	/**
	 * Return the (loaded) values of the mapped properties of the object (not including backrefs)
	 */
	Object[] getPropertyValues(Object object);

	/**
	 * Get the value of a particular property
	 */
	Object getPropertyValue(Object object, int i) throws HibernateException;

	/**
	 * Get the value of a particular property
	 */
	Object getPropertyValue(Object object, String propertyName);

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @deprecated Use {@link #getIdentifier(Object,SharedSessionContractImplementor)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"JavaDoc"})
	Serializable getIdentifier(Object object) throws HibernateException;

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @param entity The entity for which to get the identifier
	 * @param session The session from which the request originated
	 *
	 * @return The identifier
	 */
	Serializable getIdentifier(Object entity, SharedSessionContractImplementor session);

    /**
     * Inject the identifier value into the given entity.
     *
     * @param entity The entity to inject with the identifier value.
     * @param id The value to be injected as the identifier.
	 * @param session The session from which is requests originates
     */
	void setIdentifier(Object entity, Serializable id, SharedSessionContractImplementor session);

	/**
	 * Get the version number (or timestamp) from the object's version property (or return null if not versioned)
	 */
	Object getVersion(Object object) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @param id The identifier value to use (may be null to represent no value)
	 * @param session The session from which the request originated.
	 *
	 * @return The instantiated entity.
	 */
	Object instantiate(Serializable id, SharedSessionContractImplementor session);

	/**
	 * Is the given object an instance of this entity?
	 */
	boolean isInstance(Object object);

	/**
	 * Does the given instance have any uninitialized lazy properties?
	 */
	boolean hasUninitializedLazyProperties(Object object);

	/**
	 * Set the identifier and version of the given instance back to its "unsaved" value.
	 *
	 * @param entity The entity instance
	 * @param currentId The currently assigned identifier value.
	 * @param currentVersion The currently assigned version value.
	 * @param session The session from which the request originated.
	 */
	void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SharedSessionContractImplementor session);

	/**
	 * A request has already identified the entity-name of this persister as the mapping for the given instance.
	 * However, we still need to account for possible subclassing and potentially re-route to the more appropriate
	 * persister.
	 * <p/>
	 * For example, a request names <tt>Animal</tt> as the entity-name which gets resolved to this persister.  But the
	 * actual instance is really an instance of <tt>Cat</tt> which is a subclass of <tt>Animal</tt>.  So, here the
	 * <tt>Animal</tt> persister is being asked to return the persister specific to <tt>Cat</tt>.
	 * <p/>
	 * It is also possible that the instance is actually an <tt>Animal</tt> instance in the above example in which
	 * case we would return <tt>this</tt> from this method.
	 *
	 * @param instance The entity instance
	 * @param factory Reference to the SessionFactory
	 *
	 * @return The appropriate persister
	 *
	 * @throws HibernateException Indicates that instance was deemed to not be a subclass of the entity mapped by
	 * this persister.
	 */
	EntityTypeImplementor getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory);

	FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);

	/**
	 * Converts an array of attribute names to a set of indexes, according to the entity metamodel
	 *
	 * @param attributeNames Array of names to be resolved
	 *
	 * @return A set of unique indexes of the attribute names found in the metamodel
	 */
	int[] resolveAttributeIndexes(String[] attributeNames);

	boolean canUseReferenceCacheEntries();
}