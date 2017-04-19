/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.spi.CollectionType;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.spi.Type;

/**
 * An array mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class Array extends List {
	private String elementClassName;

	public Array(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public Class getElementClass() throws MappingException {
		if ( elementClassName == null ) {
			Type elementType = getElement().getType();
			return isPrimitiveArray()
					? ( (Primitive) elementType.getJavaTypeDescriptor() ).getPrimitiveClass()
					: elementType.getJavaTypeDescriptor().getJavaType();
		}
		else {
			try {
				return getMetadata().getMetadataBuildingOptions()
						.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.classForName( elementClassName );
			}
			catch (ClassLoadingException e) {
				throw new MappingException( e );
			}
		}
	}

	@Override
	public CollectionType getDefaultCollectionType() throws MappingException {
		return getMetadata().getTypeConfiguration()
				.array( getRole(), getReferencedPropertyName(), getElementClass() );
	}

	@Override
	public boolean isArray() {
		return true;
	}

	/**
	 * @return Returns the elementClassName.
	 */
	public String getElementClassName() {
		return elementClassName;
	}

	/**
	 * @param elementClassName The elementClassName to set.
	 */
	public void setElementClassName(String elementClassName) {
		this.elementClassName = elementClassName;
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}
}
