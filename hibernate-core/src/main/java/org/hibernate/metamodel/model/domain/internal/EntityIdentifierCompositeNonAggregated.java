/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.VirtualPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeNonAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements EntityIdentifier<O,J>, SingularPersistentAttribute<O,J>,
		NavigableContainer<J>, VirtualPersistentAttribute<O,J> {
	// todo : IdClass handling eventually

	public static final String NAVIGABLE_NAME = "{id}";

	private final EntityHierarchy entityHierarchy;
	private final EmbeddedTypeImplementor<J> embeddedPersister;

	@SuppressWarnings("unchecked")
	public EntityIdentifierCompositeNonAggregated(
			EntityHierarchy entityHierarchy,
			EmbeddedTypeImplementor<J> embeddedPersister) {
		super(
				entityHierarchy.getRootEntityType(),
				NAVIGABLE_NAME,
				PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess( null, NAVIGABLE_NAME ),
				embeddedPersister,
				Disposition.ID,
				false
		);
		this.entityHierarchy = entityHierarchy;
		this.embeddedPersister = embeddedPersister;
	}

	@Override
	public List<Column> getColumns() {
		return embeddedPersister.collectColumns();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttributeImplementor

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedPersister.getNavigableRole();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return embeddedPersister.getJavaTypeDescriptor();
	}

	@Override
	public String getAttributeName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeNonAggregated(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return embeddedPersister.findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return embeddedPersister.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitNonAggregatedCompositeIdentifier( entityHierarchy, embeddedPersister );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitDeclaredNavigables( visitor );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return embeddedPersister.generateQueryResult(
				selectedExpression,
				resultVariable,
				columnReferenceSource,
				sqlSelectionResolver,
				creationContext
		);
	}
}