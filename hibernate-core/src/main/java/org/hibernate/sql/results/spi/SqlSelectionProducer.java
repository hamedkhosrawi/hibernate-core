/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * @author Steve Ebersole
 */
public interface SqlSelectionProducer {
	/**
	 * Create a SqlSelection for the given JDBC ResultSet position
	 */
	SqlSelection createSqlSelection(int jdbcPosition);
}