/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.cache.jcache.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.JCacheNaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Alex Snaps
 */
public class NonStrictNaturalIdRegionAccessStrategy
		extends JCacheRegionAccessStrategy<JCacheNaturalIdRegion>
		implements NaturalIdDataAccess {

	public NonStrictNaturalIdRegionAccessStrategy(JCacheNaturalIdRegion jCacheNaturalIdRegion) {
		super( jCacheNaturalIdRegion );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		remove( session, key );
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException {
		unlockItem( session, key, lock );
		return false;
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityDescriptor persister, SharedSessionContractImplementor session) {
		return DefaultCacheKeysFactory.createNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.getNaturalIdValues( cacheKey );
	}
}
