/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.annotations.Remove;

/**
 * Used in {@link EntityInitializer} implementations
 *
 * @author Steve Ebersole
 */
@Remove
public interface EntitySqlSelectionGroup extends SqlSelectionGroup {
	SqlSelectionGroupNode getIdSqlSelections();

	SqlSelectionGroupNode getRowIdSqlSelection();

	SqlSelectionGroupNode getDiscriminatorSqlSelection();

	SqlSelectionGroupNode getTenantDiscriminatorSqlSelection();
}
