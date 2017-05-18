/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.naming.Identifier;

/**
 * Represents a physical table (or view).
 *
 * @author Steve Ebersole
 */
public class PhysicalTable extends AbstractTable implements Table {
	private final Identifier tableName;

	public PhysicalTable(Identifier tableName, boolean isAbstract) {
		super( isAbstract );
		this.tableName = tableName;
	}

	public Identifier getTableName() {
		return tableName;
	}

	@Override
	public String getTableExpression() {
		return getTableName().getText();
	}

	@Override
	public String toString() {
		return "PhysicalTable(" + tableName + ")";
	}
}