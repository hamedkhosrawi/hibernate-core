/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Specialized CacheEntry for storing direct references to entity instances.
 *
 * @author Steve Ebersole
 */
public class ReferenceCacheEntryImpl implements CacheEntry {
	private final Object reference;
	// passing the persister avoids a costly persister lookup by class name at cache retrieval time
	private final EntityTypeDescriptor subclassDescriptor;

	/**
	 * Constructs a ReferenceCacheEntryImpl
	 *
	 * @param reference The reference entity instance
	 * @param subclassDescriptor The specific subclass persister
	 */
	public ReferenceCacheEntryImpl(Object reference, EntityTypeDescriptor subclassDescriptor) {
		this.reference = reference;
		this.subclassDescriptor = subclassDescriptor;
	}

	/**
	 * Provides access to the stored reference.
	 *
	 * @return The stored reference
	 */
	public Object getReference() {
		return reference;
	}

	@Override
	public boolean isReferenceEntry() {
		return true;
	}

	@Override
	public String getSubclass() {
		return subclassDescriptor.getEntityName();
	}

	public EntityTypeDescriptor getSubclassDescriptor() {
		return subclassDescriptor;
	}

	@Override
	public Object getVersion() {
		// reference data cannot be versioned
		return null;
	}

	@Override
	public Serializable[] getDisassembledState() {
		// reference data is not disassembled into the cache
		return null;
	}
}
