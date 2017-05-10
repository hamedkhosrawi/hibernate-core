/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.sql.ast.tree.spi.from.ColumnReference;

/**
 * @author Steve Ebersole
 */
public interface ColumnReferenceResolver {
	// todo (6.0) : finish and integrate this resolver
	//		what args?  where would this
	//		see org.hibernate.sql.ast.tree.spi.expression.domain.ColumnBindingSource
	ColumnReference resolveColumnReference(Column columnBinding);
}