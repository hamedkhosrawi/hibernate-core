/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl<J>
		extends AbstractCollectionIndex<J,EntityType<J>>
		implements CollectionIndexEntity<J> {
	public CollectionIndexEntityImpl(
			CollectionPersister persister,
			IndexedCollection mappingBinding,
			EntityType<J> ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityType<J> getOrmType() {
		return super.getOrmType();
	}

	@Override
	public EntityPersister<J> getEntityPersister() {
		return getOrmType().getEntityPersister();
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
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.ENTITY;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {

	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace, SqlAliasBaseManager sqlAliasBaseManager, FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Return generateReturn(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}
}