/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.collection.spi.CollectionIndexEntity;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.EntityValuedNavigable;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmCollectionIndexReferenceEntity
		extends AbstractSqmCollectionIndexReference
		implements SqmNavigableSourceReference, SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmCollectionIndexReferenceEntity.class );

	private SqmFrom exportedFromElement;

	public SqmCollectionIndexReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public EntityValuedNavigable getReferencedNavigable() {
		return (CollectionIndexEntity) getPluralAttributeBinding().getReferencedNavigable().getCollectionPersister().getIndexDescriptor();
	}

	@Override
	public EntityValuedExpressableType getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into CollectionElementBindingEntity [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
	}

	@Override
	public EntityPersister getIntrinsicSubclassEntityPersister() {
		// todo (6.0) : override this to account for implicit or explicit Downcasts
		return super.getIntrinsicSubclassEntityPersister();
	}

	@Override
	public String getUniqueIdentifier() {
		// todo (6.0) : for the entity index classification we should point to the referenced entity's uid
		return super.getUniqueIdentifier();
	}
}