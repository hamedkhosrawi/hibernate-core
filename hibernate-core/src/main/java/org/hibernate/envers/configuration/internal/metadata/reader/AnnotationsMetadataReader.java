/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.SecondaryAuditTables;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Chris Cranford
 */
public final class AnnotationsMetadataReader {
	private final AuditMetadataBuildingOptions options;
	private final PersistentClass pc;
	private final ReflectionManager reflectionManager;
	private final XClass xclass;

	/**
	 * This object is filled with information read from annotations and returned by the <code>getVersioningData</code>
	 * method.
	 */
	private final ClassAuditingData auditData;

	public AnnotationsMetadataReader(
			AuditMetadataBuildingOptions options,
			ReflectionManager reflectionManager,
			PersistentClass pc,
			XClass xclass) {
		this.options = options;
		this.reflectionManager = reflectionManager;
		this.xclass = xclass;
		this.pc = pc;
		auditData = new ClassAuditingData();
	}

	private void addAuditTable(XClass clazz) {
		final AuditTable auditTable = clazz.getAnnotation( AuditTable.class );
		if ( auditTable != null ) {
			auditData.setAuditTable( auditTable );
		}
		else {
			auditData.setAuditTable( getDefaultAuditTable() );
		}
	}

	private void addAuditSecondaryTables(XClass clazz) {
		// Getting information on secondary tables
		final SecondaryAuditTable secondaryVersionsTable1 = clazz.getAnnotation( SecondaryAuditTable.class );
		if ( secondaryVersionsTable1 != null ) {
			auditData.getSecondaryTableDictionary().put(
					secondaryVersionsTable1.secondaryTableName(),
					secondaryVersionsTable1.secondaryAuditTableName()
			);
		}

		final SecondaryAuditTables secondaryAuditTables = clazz.getAnnotation( SecondaryAuditTables.class );
		if ( secondaryAuditTables != null ) {
			for ( SecondaryAuditTable secondaryAuditTable2 : secondaryAuditTables.value() ) {
				auditData.getSecondaryTableDictionary().put(
						secondaryAuditTable2.secondaryTableName(),
						secondaryAuditTable2.secondaryAuditTableName()
				);
			}
		}
	}

	public ClassAuditingData getAuditData() {
		if ( xclass.isAnnotationPresent( Audited.class ) ) {
			auditData.setDefaultAudited( true );
		}

		new AuditedPropertiesReader(
				new PersistentClassPropertiesSource( xclass ),
				auditData,
				options,
				reflectionManager,
				""
		).read();

		addAuditTable( xclass );
		addAuditSecondaryTables( xclass );

		return auditData;
	}

	private AuditTable defaultAuditTable = new AuditTable() {
		public String value() {
			return "";
		}

		public String schema() {
			return "";
		}

		public String catalog() {
			return "";
		}

		public Class<? extends Annotation> annotationType() {
			return this.getClass();
		}
	};

	private AuditTable getDefaultAuditTable() {
		return defaultAuditTable;
	}

	private class PersistentClassPropertiesSource implements PersistentPropertiesSource {
		private final XClass xclass;

		private PersistentClassPropertiesSource(XClass xclass) {
			this.xclass = xclass;
		}

		@SuppressWarnings({"unchecked"})
		public Iterator<Property> getPropertyIterator() {
			return pc.getPropertyIterator();
		}

		public Property getProperty(String propertyName) {
			return pc.getProperty( propertyName );
		}

		public XClass getXClass() {
			return xclass;
		}
	}
}
