/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

/**
 * Collection second pass
 *
 * @author Emmanuel Bernard
 */
public abstract class CollectionSecondPass implements SecondPass {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionSecondPass.class.getName());

	MetadataBuildingContext buildingContext;
	Collection collection;
	private Map localInheritedMetas;

	public CollectionSecondPass(MetadataBuildingContext buildingContext, Collection collection, java.util.Map inheritedMetas) {
		this.collection = collection;
		this.buildingContext = buildingContext;
		this.localInheritedMetas = inheritedMetas;
	}

	public CollectionSecondPass(MetadataBuildingContext buildingContext, Collection collection) {
		this( buildingContext, collection, Collections.EMPTY_MAP );
	}

	public void doSecondPass(java.util.Map<String, PersistentClass> persistentClasses)
			throws MappingException {
		final boolean debugEnabled = LOG.isDebugEnabled();
		if ( debugEnabled ) {
			LOG.debugf( "Second pass for collection: %s", collection.getRole() );
		}

		secondPass( persistentClasses, localInheritedMetas ); // using local since the inheritedMetas at this point is not the correct map since it is always the empty map
		collection.createAllKeys();

		if ( debugEnabled ) {
			String msg = "Mapped collection key: " + columns( collection.getKey() );
			if ( collection.isIndexed() )
				msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
			if ( collection.isOneToMany() ) {
				msg += ", one-to-many: "
					+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			}
			else {
				msg += ", element: " + columns( collection.getElement() );
			}
			LOG.debug( msg );
		}
	}

	abstract public void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
			throws MappingException;

	private static String columns(Value<?> val) {
		final StringBuilder columns = new StringBuilder();
		List<MappedColumn> mappedColumns = val.getMappedColumns();
		final int numberOfColumns = mappedColumns.size();
		for ( int i = 0; i < numberOfColumns - 2; i++ ) {
			columns.append( mappedColumns.get( i ).getText() );
			columns.append( ", " );
		}
		columns.append( mappedColumns.get( numberOfColumns - 1 ).getText() );
		return columns.toString();
	}
}
