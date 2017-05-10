/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.common.spi.VirtualPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.ast.tree.spi.from.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SelectableEmbeddedTypeImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeNonAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J>, NavigableSource<J>, VirtualPersistentAttribute<O,J> {
	// todo : IdClass handling eventually

	public static final String NAVIGABLE_NAME = "{id}";

	private final EntityHierarchy entityHierarchy;
	private final EmbeddedPersister<J> embeddedPersister;

	@SuppressWarnings("unchecked")
	public IdentifierDescriptorCompositeNonAggregated(
			EntityHierarchy entityHierarchy,
			EmbeddedPersister<J> embeddedPersister) {
		super(
				entityHierarchy.getRootEntityPersister(),
				NAVIGABLE_NAME,
				PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess( null, NAVIGABLE_NAME ),
				embeddedPersister.getOrmType(),
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
	public EmbeddedType getIdType() {
		return embeddedPersister.getOrmType();
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

	@Override
	public Return generateReturn(QueryResultCreationContext returnResolutionContext, TableGroup tableGroup) {
		// todo : not sure what we will need here yet...

		// for now...
		return new SelectableImpl( this, returnResolutionContext, tableGroup ).toQueryReturn( returnResolutionContext, null );
	}

	@Override
	public Fetch generateFetch(QueryResultCreationContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
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
		return "IdentifierCompositeNonAggregated(" + getSource().asLoggableText() + ")";
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
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return getSource().resolveJoinColumnMappings( persistentAttribute );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	private static class SelectableImpl implements Selectable, NavigableReferenceExpression {
		private final SingularAttributeReferenceExpression expressionDelegate;
		private final SelectableEmbeddedTypeImpl selectableDelegate;
		private final NavigablePath navigablePath;

		public SelectableImpl(
				IdentifierDescriptorCompositeNonAggregated idDescriptor,
				QueryResultCreationContext returnResolutionContext,
				TableGroup tableGroup) {
			this.navigablePath = returnResolutionContext.currentNavigablePath().append( idDescriptor.getNavigableName() );

			this.expressionDelegate = new SingularAttributeReferenceExpression(
					tableGroup,
					idDescriptor,
					navigablePath
			);
			this.selectableDelegate = new SelectableEmbeddedTypeImpl(
					this,
					getColumnReferences(),
					getType()
			);
		}

		@Override
		public EmbeddedType getType() {
			return (EmbeddedType) expressionDelegate.getType();
		}

		@Override
		public Selectable getSelectable() {
			return this;
		}

		@Override
		public void accept(SqlSelectAstToJdbcSelectConverter walker) {
			// todo (6.0) : do we need a separate "visitEntityIdentifier" method(s)?

			walker.visitSingularAttributeReference( expressionDelegate );
		}

		@Override
		public Expression getSelectedExpression() {
			return expressionDelegate;
		}

		@Override
		public Return toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
			return selectableDelegate.toQueryReturn( returnResolutionContext, resultVariable );
		}

		@Override
		public Navigable getNavigable() {
			return expressionDelegate.getNavigable();
		}

		@Override
		public NavigablePath getNavigablePath() {
			return expressionDelegate.getNavigablePath();
		}

		@Override
		public List<ColumnReference> getColumnReferences() {
			return expressionDelegate.getColumnReferences();
		}
	}
}