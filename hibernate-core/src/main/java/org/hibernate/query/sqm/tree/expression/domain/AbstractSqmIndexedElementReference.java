/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.collection.spi.CollectionElement;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.queryable.spi.NavigableSourceReferenceInfo;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmIndexedElementReference
		extends AbstractSpecificSqmElementReference
		implements SqmRestrictedCollectionElementReference {
	private final SqmExpression indexSelectionExpression;
	private final NavigablePath propertyPath;

	public AbstractSqmIndexedElementReference(
			SqmPluralAttributeReference pluralAttributeBinding,
			SqmExpression indexSelectionExpression) {
		super( pluralAttributeBinding );
		this.indexSelectionExpression = indexSelectionExpression;
		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{indexes}" );
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return getPluralAttributeBinding();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeBinding().getReferencedNavigable().getCollectionPersister().getElementDescriptor();
	}

	@Override
	public CollectionElement getExpressionType() {
		return (CollectionElement) getReferencedNavigable();
	}

	@Override
	public CollectionElement getInferableType() {
		return getExpressionType();
	}

	@Override
	public String asLoggableText() {
		return propertyPath.getFullPath();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public NavigableSourceReferenceInfo getSourceReferenceInfo() {
		return getPluralAttributeBinding();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most element classifications, the uid should point to the "collection table"...
		return getPluralAttributeBinding().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most element classifications, the "identification variable" (alias)
		// 		associated with elements is the identification variable for the collection reference
		return getPluralAttributeBinding().getIdentificationVariable();
	}

	@Override
	public EntityPersister getIntrinsicSubclassEntityPersister() {
		// for most element classifications, there is none
		return null;
	}
}