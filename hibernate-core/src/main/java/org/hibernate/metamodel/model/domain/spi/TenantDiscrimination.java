/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * @author Steve Ebersole
 */
public interface TenantDiscrimination extends VirtualNavigable<String>, BasicValuedNavigable<String> {
	boolean isShared();

	boolean isUseParameterBinding();

	// todo (6.0) : VirtualNavigable?
	//		see note on VirtualPersistentAttribute

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitTenantTenantDiscrimination( this );
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default Class<String> getJavaType() {
		return String.class;
	}
}
