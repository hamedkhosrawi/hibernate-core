/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * @author Steve Ebersole
 * @author Dmytro Bondar
 */
public interface ImplicitIndexColumnNameSource extends ImplicitNameSource {

	AttributePath getPluralAttributePath();

}
