/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * concrete ManagedType implementations (entity and embedded) are modelled as a
 * "descriptor" (see {@link EntityDescriptor} and {@link EmbeddedTypeDescriptor}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "descriptor" :
 * {@link PersistentCollectionDescriptor}.
 *
 * @todo (6.0) : describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<T>
		extends ManagedType<T>, NavigableContainer<T>, EmbeddedContainer<T>, ExpressableType<T> {

	TypeConfiguration getTypeConfiguration();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	RepresentationStrategy getRepresentationStrategy();

	PersistentAttribute<? super T, ?> findAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name, Class resultType);

	default List<PersistentAttribute<? super T,?>> getPersistentAttributes() {
		final List<PersistentAttribute<? super T,?>> attributes = new ArrayList<>();
		collectAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	default List<PersistentAttribute<? super T, ?>> getDeclaredPersistentAttributes() {
		final List<PersistentAttribute<? super T,?>> attributes = new ArrayList<>();
		collectDeclaredAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	Map<String, PersistentAttribute> getAttributesByName();
	Map<String, PersistentAttribute> getDeclaredAttributesByName();

	default void visitAttributes(Consumer<? extends PersistentAttribute> consumer) {
		throw new NotYetImplementedFor6Exception();
	}

	<A extends javax.persistence.metamodel.Attribute> void collectAttributes(Consumer<A> collector, Class<A> restrictionType);
	<A extends javax.persistence.metamodel.Attribute> void collectDeclaredAttributes(Consumer<A> collector, Class<A> restrictionType);

	void visitStateArrayNavigables(Consumer<StateArrayValuedNavigable<?>> consumer);


	/**
	 * Reduce an instance of the described entity into its "values array" -
	 * an array whose length is equal to the number of attributes where the
	 * `includeCondition` tests {@code true}.  Each element corresponds to either:
	 *
	 * 		* if the passed `swapCondition` tests {@code true}, then
	 * 			the value passed as `swapValue`
	 * 		* otherwise the attribute's extracted value
	 *
	 * In more specific terms, this method is responsible for extracting the domain
	 * object's value state array - which is the form we use in many places such
	 * EntityEntry#loadedState, L2 cache entry, etc.
	 *
	 * @param instance An instance of the described type (this)
	 * @param includeCondition Predicate to see if the given sub-Navigable should create
	 * an index in the array being built.
	 * @param swapCondition Predicate to see if the sub-Navigable's reduced state or
	 * the passed `swapValue` should be used for that sub-Navigable's value as its
	 * element in the array being built
	 * @param swapValue The value to use if the passed `swapCondition` tests {@code true}
	 * @param session The session :)
	 */
	default Object[] reduceToValuesArray(
			T instance,
			Predicate<PersistentAttribute> includeCondition,
			Predicate<PersistentAttribute> swapCondition,
			Object swapValue,
			SharedSessionContractImplementor session) {
		final ArrayList<Object> values = new ArrayList<>();

		// todo (6.0) : the real trick here is deciding which values to put in the array.
		//		specifically how to handle values like version, discriminator, etc
		//
		//		do we put that onus on the `includeCondition` completely (external)?
		//		or is this something that the descriptor should handle? maybe a method
		//
		//		generally speaking callers only care about the `swapCondition` which is
		//		where the "insertability", "laziness", etc comes into play

		// todo (6.0) : one option for this (^^) is to define `includeCondition` and `swapCondition` as `Predicate<Navigable` instead
		//		ManagedTypeDescriptor's implementation of that would walk these Navigables[1]
		//
		// [1] whichever Navigables we decide needs to be there in whatever order we decide.. it just needs to be consistent in usage[2]
		// [2] possibly (hopefully!)this (^^) can hold true for our enhancement needs as well.  A possible solution for would be
		// 		to just "reserve" the first few elements of this array for root-entity state such as id, version, discriminator, etc

		// todo (7.0) : bytecode enhancement should use some facilities to build a `org.hibernate.boot.Metadata` reference to determine its strategy for enhancement.
		//		this is related to the 2 6.0 todo comments above
		//
		//		drawback to this approach is that it would miss any provided XML overrides/additions.  - is that reasonable?
		//		maybe a comprise is to say that we can enhance anything for which there are XML *overrides*, but not additions
		// 		such as adding new entity definitions (the new ones would not be hooked

		visitAttributes(
				attribute -> {
					if ( includeCondition.test( attribute ) ) {
						values.add(
								swapCondition.test( attribute )
										? swapValue
										: attribute.getPropertyAccess().getGetter().get( instance )
						);
					}
				}
		);
		return values.toArray();
	}

	default Object extractAttributeValue(T instance, PersistentAttribute attribute) {
		return attribute.getPropertyAccess().getGetter().get( instance );
	}

	default void injectAttributeValue(T instance, PersistentAttribute attribute, Object value) {
		attribute.getPropertyAccess().getSetter().set( instance, value, getTypeConfiguration().getSessionFactory() );
	}
}