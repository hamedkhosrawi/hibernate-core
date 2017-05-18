/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.queryable.spi;

import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public interface TableGroupResolver {
	/**
	 * Resolve a TableGroup by its unique identifier
	 */
	TableGroup resolveTableGroup(String uid);

	ColumnReferenceSource resolveColumnReferenceSource(String uid);
	NavigableReferenceInfo resolveNavigableReferenceInfo(String uid);
}