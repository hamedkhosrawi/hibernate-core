/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.lang.reflect.Field;
import java.util.Locale;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * todo (6.0) : this needs to be a SqmExpression
 *
 * @author Steve Ebersole
 */
public class SemanticPathPartNamedField implements SemanticPathPart {
	private final Field theField;

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartNamedField(Field theField) {
		this.theField = theField;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException(
				String.format(
						Locale.ROOT,
						"A field [%s.%s] cannot be further de-referenced",
						theField.getDeclaringClass().getName(),
						theField.getName()
				)
		);
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException(
				String.format(
						Locale.ROOT,
						"A field [%s.%s] cannot be further de-referenced",
						theField.getDeclaringClass().getName(),
						theField.getName()
				)
		);
	}
}
