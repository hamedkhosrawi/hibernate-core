/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer {

	// per-row state
	private PersistentCollection collectionInstance;

	public DelayedCollectionInitializer(
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			PersistentCollectionDescriptor fetchCollectionDescriptor,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		super( fetchCollectionDescriptor, parentAccess, navigablePath, false, keyContainerAssembler, keyCollectionAssembler );
	}

	@Override
	public PersistentCollection getCollectionInstance() {
		return collectionInstance;
	}

	@Override
	public void resolve(RowProcessingState rowProcessingState) {
		// nothing to do - the collection is lazy or found already managed in Session

		final CollectionKey collectionKey = resolveCollectionKey( rowProcessingState );

		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		final PersistentCollectionDescriptor collectionDescriptor = getFetchedAttribute().getPersistentCollectionDescriptor();

		final PersistentCollection existing = persistenceContext.getCollection( collectionKey );
		if ( existing != null ) {
			collectionInstance = existing;
		}
		else {
			collectionInstance = collectionDescriptor.instantiateWrapper(
					session,
					collectionKey.getKey()
			);

			persistenceContext.addUninitializedCollection( collectionDescriptor, collectionInstance, collectionKey.getKey() );
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		collectionInstance = null;
	}
}
