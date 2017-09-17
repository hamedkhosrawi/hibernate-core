/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Represents the commonality between {@link PhysicalColumn} and {@link DerivedColumn}
 *
 * @author Steve Ebersole
 */
public interface Column extends QualifiableSqlExpressable {
	Table getSourceTable();

	String getExpression();

	// todo : nullable, size, etc

	// todo : org.hibernate.annotations.ColumnTransformer#read and org.hibernate.annotations.ColumnTransformer#write?

	String toLoggableString();

	String render(String identificationVariable);

	SqlTypeDescriptor getSqlTypeDescriptor();

	@Override
	default Expression createSqlExpression(ColumnReferenceQualifier qualifier) {
		return new ColumnReference( qualifier, this );
	}

	/**
	 * @deprecated Use {@link #getSqlTypeDescriptor()} instead
	 */
	@Deprecated
	default int getJdbcType() {
		return getSqlTypeDescriptor().getJdbcTypeCode();
	}

}