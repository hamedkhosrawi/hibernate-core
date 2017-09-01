/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Factory that does not fill in the entityName or role
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class SimpleCacheKeysFactory implements CacheKeysFactory {

public static CacheKeysFactory INSTANCE = new SimpleCacheKeysFactory();

	@Override
	public Object createEntityKey(
			Object id,
			EntityHierarchy entityHierarchy,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return id;
	}

	@Override
	public Object createNaturalIdKey(
			Object[] naturalIdValues,
			EntityHierarchy entityHierarchy,
			SharedSessionContractImplementor session) {
		// natural ids always need to be wrapped
		return new OldNaturalIdCacheKey( naturalIdValues, entityHierarchy, session );
	}

	@Override
	public Object createCollectionKey(
			Object id,
			PersistentCollectionDescriptor persister,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return id;
	}

	@Override
	public Object getEntityId(Object cacheKey) {
		return cacheKey;
	}

	@Override
	public Object getCollectionId(Object cacheKey) {
		return cacheKey;
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return ((OldNaturalIdCacheKey) cacheKey).getNaturalIdValues();
	}
}
