/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class PkDrivenByDefaultMapsIdSecondPass implements SecondPass {
	private final String referencedEntityName;
	private final Ejb3JoinColumn[] columns;
	private final SimpleValue value;

	public PkDrivenByDefaultMapsIdSecondPass(String referencedEntityName, Ejb3JoinColumn[] columns, SimpleValue value) {
		this.referencedEntityName = referencedEntityName;
		this.columns = columns;
		this.value = value;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		PersistentClass referencedEntity = (PersistentClass) persistentClasses.get( referencedEntityName );
		if ( referencedEntity == null ) {
			throw new AnnotationException(
					"Unknown entity name: " + referencedEntityName
			);
		}
		TableBinder.linkJoinColumnWithValueOverridingNameIfImplicit(
				referencedEntity,
				referencedEntity.getKey().getMappedColumns(),
				columns,
				value);
	}
}
