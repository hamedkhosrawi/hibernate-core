/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.Locale;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.BasicType;

/**
 * Represents a binding of a column (derived or physical) into a SQL statement
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements SqlSelectable {
	private final String identificationVariable;
	private final Column column;
	private final SqlSelectionReader sqlSelectionReader;

	public ColumnReference(Column column, BasicType type, TableReference tableBinding) {
		this.identificationVariable = tableBinding.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( type );
	}

	public ColumnReference(Column column, int jdbcTypeCode, TableReference tableBinding) {
		this.identificationVariable = tableBinding.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( jdbcTypeCode );
	}

	public ColumnReference(Column column, TableReference tableBinding) {
		this( column, column.getJdbcType(), tableBinding );
	}

	public Column getColumn() {
		return column;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return sqlSelectionReader;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter interpreter) {
		interpreter.visitColumnBinding( this );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ColumnReference that = (ColumnReference) o;
		return getIdentificationVariable().equals( that.getIdentificationVariable() )
				&& getColumn().equals( that.getColumn() );
	}

	@Override
	public int hashCode() {
		int result = getIdentificationVariable().hashCode();
		result = 31 * result + getColumn().hashCode();
		return result;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT, 
				"ColumnBinding(%s.%s)",
				getIdentificationVariable(),
				column.getExpression()
		);
	}
}