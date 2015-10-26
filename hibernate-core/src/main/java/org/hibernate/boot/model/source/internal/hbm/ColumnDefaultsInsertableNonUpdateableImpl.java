/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

/**
 * @author Steve Ebersole
 */
class ColumnDefaultsInsertableNonUpdateableImpl implements ColumnDefaults {
	/**
	 * Singleton access
	 */
	public static final ColumnDefaultsInsertableNonUpdateableImpl INSTANCE = new ColumnDefaultsInsertableNonUpdateableImpl();

	@Override
	public Boolean isNullable() {
		return null;
	}

	@Override
	public Integer getLength() {
		return null;
	}

	@Override
	public Integer getScale() {
		return null;
	}

	@Override
	public Integer getPrecision() {
		return null;
	}

	@Override
	public Boolean isUnique() {
		return null;
	}

	@Override
	public Boolean isInsertable() {
		return true;
	}

	@Override
	public Boolean isUpdateable() {
		return false;
	}
}
