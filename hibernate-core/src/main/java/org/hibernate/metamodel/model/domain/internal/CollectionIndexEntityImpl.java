/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEntity<J> {
	private final EntityTypeImplementor<J> entityPersister;
	private final NavigableRole navigableRole;

	public CollectionIndexEntityImpl(
			PersistentCollectionMetadata persister,
			IndexedCollection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		this.entityPersister = null;
		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public EntityTypeImplementor<J> getEntityPersister() {
		return entityPersister;
	}

	@Override
	public String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityPersister().getJpaEntityName();
	}

	@Override
	public IndexClassification getClassification() {
		// todo : distinguish between OneToMany and ManyToMany
		return IndexClassification.ONE_TO_MANY;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEntityPersister().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEntityPersister().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getEntityPersister().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityPersister().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getEntityPersister().visitDeclaredNavigables( visitor );
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBaseManager.SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector) {
		getEntityPersister().applyTableReferenceJoins( joinType, sqlAliasBase, collector );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityPersister().getJavaTypeDescriptor();
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultEntityImpl(
				selectedExpression,
				entityPersister,
				resultVariable,
				null,
				selectedExpression.getNavigablePath(),
				columnReferenceSource
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceResolver,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return null;
	}
}