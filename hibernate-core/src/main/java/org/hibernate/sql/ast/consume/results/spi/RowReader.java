/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.spi;

import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public interface RowReader<R> {
	R readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options) throws SQLException;

	void finishUp(JdbcValuesSourceProcessingState context);
}
