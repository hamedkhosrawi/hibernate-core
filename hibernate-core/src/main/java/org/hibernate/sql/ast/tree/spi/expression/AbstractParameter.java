/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.ast.produce.result.internal.ReturnScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.sqm.spi.ParameterSpec;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.JdbcParameterBinder;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameter
		implements ParameterSpec, JdbcParameterBinder, Expression, SqlSelectable, Selectable {
	private static final Logger log = Logger.getLogger( AbstractParameter.class );

	private final ExpressableType inferredType;

	public AbstractParameter(ExpressableType inferredType) {
		this.inferredType = inferredType;
	}

	public ExpressableType getInferredType() {
		return inferredType;
	}

	@Override
	public ExpressableType getType() {
		return getInferredType();
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Expression getSelectedExpression() {
		return this;
	}

	@Override
	public Return toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				this,
				returnResolutionContext.resolveSqlSelection( this ),
				resultVariable,
				getType()
		);
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicType) getType() );
	}

	protected int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBinding valueBinding,
			SharedSessionContractImplementor session) throws SQLException {
		final ExpressableType bindType;
		final Object bindValue;

		if ( valueBinding == null ) {
			warnNoBinding();
			bindType = valueBinding.getBindType();
			bindValue = null;
		}
		else {
			if ( valueBinding.getBindType() == null ) {
				bindType = inferredType;
			}
			else {
				bindType = valueBinding.getBindType();
			}
			bindValue = valueBinding.getBindValue();
		}

		if ( bindType == null ) {
			unresolvedType();
		}
		assert bindType != null;
		if ( bindValue == null ) {
			warnNullBindValue();
		}

		bindType.nullSafeSet( statement, bindValue, startPosition, session );
		return bindType.getColumnSpan();
	}

	protected abstract void warnNoBinding();

	protected abstract void unresolvedType();

	protected abstract void warnNullBindValue();
}