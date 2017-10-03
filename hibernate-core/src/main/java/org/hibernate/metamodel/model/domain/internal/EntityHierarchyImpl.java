/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private static final Logger log = Logger.getLogger( EntityHierarchyImpl.class );

	private final EntityDescriptor rootEntityPersister;

	private final InheritanceStrategy inheritanceStrategy;
	private final OptimisticLockStyle optimisticLockStyle;
	private final RepresentationMode representationMode;

	private final EntityIdentifier identifierDescriptor;
	private final DiscriminatorDescriptor discriminatorDescriptor;
	private final VersionDescriptor versionDescriptor;
	private final NaturalIdDescriptor naturalIdentifierDescriptor;
	private final RowIdDescriptor rowIdDescriptor;
	private final TenantDiscrimination tenantDiscrimination;

	private final String whereFragment;
	private final boolean mutable;
	private final boolean implicitPolymorphismEnabled;

	private EntityDataAccess caching;

	public EntityHierarchyImpl(
			RuntimeModelCreationContext creationContext,
			EntityDescriptor rootEntityPersister,
			RootClass rootEntityBinding) {
		log.debugf( "Creating EntityHierarchy root EntityPersister : %s", rootEntityPersister );


		this.rootEntityPersister = rootEntityPersister;

		this.inheritanceStrategy = interpretInheritanceType( rootEntityBinding );
		this.optimisticLockStyle = rootEntityBinding.getEntityMappingHierarchy().getOptimisticLockStyle();
		this.representationMode = determineRepresentationMode( rootEntityBinding, rootEntityPersister, creationContext );

		this.identifierDescriptor = interpretIdentifierDescriptor( this, rootEntityBinding, rootEntityPersister, creationContext );
		this.discriminatorDescriptor = interpretDiscriminatorDescriptor( this, rootEntityBinding, creationContext );
		this.versionDescriptor = interpretVersionDescriptor( this, rootEntityBinding, creationContext );
		this.rowIdDescriptor = interpretRowIdDescriptor( this, rootEntityBinding, creationContext );
		this.tenantDiscrimination = interpretTenantDiscrimination( this, rootEntityBinding, creationContext );
		this.naturalIdentifierDescriptor = interpretNaturalIdentifierDescriptor( this, rootEntityBinding, creationContext );

		this.whereFragment = rootEntityBinding.getWhere();
		this.mutable = rootEntityBinding.isMutable();
		this.implicitPolymorphismEnabled = !rootEntityBinding.isExplicitPolymorphism();
	}

	private RepresentationMode determineRepresentationMode(
			RootClass rootEntityBinding,
			EntityDescriptor rootEntityPersister,
			RuntimeModelCreationContext creationContext) {
		// see if a specific one was requested specific to this hierarchy
		if ( rootEntityBinding.getExplicitRepresentationMode() != null ) {
			return rootEntityBinding.getExplicitRepresentationMode();
		}

		// otherwise,
		//
		// if there is no corresponding Java type, assume MAP mode
		if ( rootEntityPersister.getJavaTypeDescriptor().getJavaType() == null ) {
			return RepresentationMode.MAP;
		}


		// assume POJO
		return RepresentationMode.POJO;
	}

	private static InheritanceStrategy interpretInheritanceType(RootClass rootEntityBinding) {
		if ( rootEntityBinding.getSubTypeMappings().isEmpty() ) {
			return InheritanceStrategy.NONE;
		}
		else {
			final Subclass subEntityBinding = (Subclass) rootEntityBinding.getSubTypeMappings().iterator().next();
			if ( subEntityBinding instanceof UnionSubclass ) {
				return InheritanceStrategy.UNION;
			}
			else if ( subEntityBinding instanceof JoinedSubclass ) {
				return InheritanceStrategy.JOINED;
			}
			else {
				return InheritanceStrategy.DISCRIMINATOR;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static EntityIdentifier interpretIdentifierDescriptor(
			EntityHierarchyImpl runtimeModelHierarchy,
			RootClass bootModelRootEntity,
			EntityDescriptor runtimeModelRootEntity,
			RuntimeModelCreationContext creationContext) {
		if ( bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping() != null ) {

			// should mean we have a "non-aggregated composite-id" (what we
			// 		historically called an "embedded composite id")
			return new EntityIdentifierCompositeNonAggregatedImpl(
					runtimeModelHierarchy,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelRootEntity,
							bootModelRootEntity.getIdentifierProperty().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					bootModelRootEntity.getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping()
			);
		}
		else if ( bootModelRootEntity.getIdentifier() instanceof EmbeddedValueMappingImplementor ) {
			// indicates we have an aggregated composite identifier (should)
			assert !bootModelRootEntity.getIdentifierProperty().isOptional();

			return  new EntityIdentifierCompositeAggregatedImpl(
					runtimeModelHierarchy,
					bootModelRootEntity,
					( (EmbeddedValueMappingImplementor) bootModelRootEntity.getIdentifier() ).makeRuntimeDescriptor(
							runtimeModelHierarchy.getRootEntityType(),
							bootModelRootEntity.getIdentifierProperty().getName(),
							SingularPersistentAttribute.Disposition.ID,
							creationContext
					),
					creationContext
			);
		}
		else {
			// should indicate a simple identifier
			assert !bootModelRootEntity.getIdentifierProperty().isOptional();

			return new EntityIdentifierSimpleImpl(
					runtimeModelHierarchy,
					bootModelRootEntity,
					creationContext
			);
		}
	}

	private static RowIdDescriptor interpretRowIdDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getRootTable().getRowId() != null ) {
			return new RowIdDescriptorImpl( hierarchy, creationContext );
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static DiscriminatorDescriptor interpretDiscriminatorDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		creationContext.getDatabaseObjectResolver().resolveTable( rootEntityBinding.getRootTable() );
		if ( rootEntityBinding.getDiscriminator() == null ) {
			return null;
		}

		return new DiscriminatorDescriptorImpl(
				hierarchy,
				(BasicValueMapping) rootEntityBinding.getDiscriminator(),
				creationContext
		);

	}

	@SuppressWarnings("unchecked")
	private static VersionDescriptor interpretVersionDescriptor(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( rootEntityBinding.getVersion() == null ) {
			return null;
		}

		return new VersionDescriptorImpl(
				hierarchy,
				rootEntityBinding,
				creationContext
		);
	}

	private static TenantDiscrimination interpretTenantDiscrimination(
			EntityHierarchyImpl hierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		return null;
	}

	private static NaturalIdDescriptor interpretNaturalIdentifierDescriptor(
			EntityHierarchyImpl entityHierarchy,
			RootClass rootEntityBinding,
			RuntimeModelCreationContext creationContext) {
		if ( !rootEntityBinding.hasNaturalId() ) {
			return null;
		}

		return new NaturalIdDescriptor() {

			// todo (6.0) : create NaturalIdDescriptorImpl

			private NaturalIdDataAccess cacheAccess;

			@Override
			public Collection<PersistentAttribute> getPersistentAttributes() {
				throw new NotYetImplementedException(  );
			}

			@Override
			public Object[] resolveSnapshot(Object entityId, SharedSessionContractImplementor session) {
				return new Object[0];
			}

			@Override
			public boolean isMutable() {
				// todo (6.0) : boot model needs to expose whether the natural-id is mutable
				return true;
			}

			@Override
			public NaturalIdDataAccess getCacheAccess() {
				return entityHierarchy.getRootEntityType().getFactory().getCache().getNaturalIdRegionAccess( entityHierarchy );
			}
		};
	}


	@Override
	public void finishInitialization(RuntimeModelCreationContext creationContext, RootClass mappingType) {
		// anything to do?
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityDescriptor getRootEntityType() {
		return rootEntityPersister;
	}

	@Override
	public RepresentationMode getRepresentation() {
		return representationMode;
	}

	@Override
	public InheritanceStrategy getInheritanceStrategy() {
		return inheritanceStrategy;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityIdentifier getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public DiscriminatorDescriptor getDiscriminatorDescriptor() {
		return discriminatorDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public VersionDescriptor getVersionDescriptor() {
		return versionDescriptor;
	}

	@Override
	public NaturalIdDescriptor getNaturalIdDescriptor() {
		return naturalIdentifierDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RowIdDescriptor getRowIdDescriptor() {
		return rowIdDescriptor;
	}

	@Override
	public TenantDiscrimination getTenantDiscrimination() {
		return tenantDiscrimination;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public EntityDataAccess getEntityCacheAccess() {
		if ( caching == null ) {
			caching = rootEntityPersister.getFactory().getCache().getEntityRegionAccess( this );
		}
		return caching;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isImplicitPolymorphismEnabled() {
		return implicitPolymorphismEnabled;
	}

	@Override
	public String getWhere() {
		return whereFragment;
	}
}