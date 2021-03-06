/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityGraph;

import org.hibernate.Filter;
import org.hibernate.UnknownProfileException;
import org.hibernate.annotations.Remove;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

/**
 * Centralize all options which can be enabled on the Session in order to
 * influence the SQL query needed to load an entity.  Currently such
 * influencers are defined as:<ul>
 *     <li>filters</li>
 *     <li>fetch profiles</li>
 *     <li>internal fetch profile (see {@link InternalFetchProfileType})</li>
 *     <li>(non default) entity graph</li>
 * </ul>
 *
 * todo (6.0) consider using the paradigm of an "adjuster"
 * 		- each potential influencer would define its own adjuster impl
 * 		- resolution regarding which of these would follow some precedence order tbd
 * 		- would affect (1) SQL AST and (2) Loader internals (possibly different impls based on these).
 * 				^^ however need to account for possibility of multiple of these being in effect.
 *
 * @author Steve Ebersole
 */
public class LoadQueryInfluencers implements Serializable {
	/**
	 * Static reference useful for cases where we are creating load SQL
	 * outside the context of any influencers.  One such example is
	 * anything created by the session factory.
	 */
	public static final LoadQueryInfluencers NONE = new LoadQueryInfluencers();

	private final SessionFactoryImplementor sessionFactory;
	private InternalFetchProfileType enabledInternalFetchProfileType;
	private final Map<String,Filter> enabledFilters;
	private final Set<String> enabledFetchProfileNames;

	private final EffectiveEntityGraph effectiveEntityGraph = new EffectiveEntityGraph();

	public LoadQueryInfluencers() {
		this( null );
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, new HashMap<>(), new HashSet<>() );
	}

	private LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, Map<String,Filter> enabledFilters, Set<String> enabledFetchProfileNames) {
		this.sessionFactory = sessionFactory;
		this.enabledFilters = enabledFilters;
		this.enabledFetchProfileNames = enabledFetchProfileNames;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}


	// internal fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public enum InternalFetchProfileType {
		MERGE( "merge" ),
		REFRESH( "refresh" );

		private final String legacyName;

		InternalFetchProfileType(String legacyName) {
			this.legacyName = legacyName;
		}

		public String getLegacyName() {
			return legacyName;
		}

		public static InternalFetchProfileType fromLegacyName(String legacyName) {
			if ( StringHelper.isEmpty( legacyName ) ) {
				return null;
			}

			if ( MERGE.legacyName.equalsIgnoreCase( legacyName ) ) {
				return MERGE;
			}

			if ( REFRESH.legacyName.equalsIgnoreCase( legacyName ) ) {
				return REFRESH;
			}

			throw new IllegalArgumentException(
					"Passed name [" + legacyName + "] not recognized as a legacy internal fetch profile name; " +
							"supported values include: 'merge' and 'refresh'"
			);
		}
	}

	public InternalFetchProfileType getEnabledInternalFetchProfileType() {
		return enabledInternalFetchProfileType;
	}

	public void setEnabledInternalFetchProfileType(InternalFetchProfileType enabledInternalFetchProfileType) {
		if ( sessionFactory == null ) {
			// thats the signal that this is the immutable, context-less
			// variety
			throw new IllegalStateException( "Cannot modify context-less LoadQueryInfluencers" );
		}

		this.enabledInternalFetchProfileType = enabledInternalFetchProfileType;
	}

	/**
	 * @deprecated Use {@link #getEnabledInternalFetchProfileType} instead
	 */
	@Deprecated
	public String getInternalFetchProfile() {
		return getEnabledInternalFetchProfileType().legacyName;
	}

	/**
	 * @deprecated Use {@link #setEnabledInternalFetchProfileType} instead
	 */
	@Deprecated
	public void setInternalFetchProfile(String internalFetchProfile) {
		setEnabledInternalFetchProfileType( InternalFetchProfileType.fromLegacyName( internalFetchProfile ) );
	}


	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFilters() {
		return !enabledFilters.isEmpty();
	}

	public Map<String,Filter> getEnabledFilters() {
		// First, validate all the enabled filters...
		//TODO: this implementation has bad performance
		for ( Filter filter : enabledFilters.values() ) {
			filter.validate();
		}
		return enabledFilters;
	}

	/**
	 * Returns an unmodifiable Set of enabled filter names.
	 * @return an unmodifiable Set of enabled filter names.
	 */
	public Set<String> getEnabledFilterNames() {
		return java.util.Collections.unmodifiableSet( enabledFilters.keySet() );
	}

	public Filter getEnabledFilter(String filterName) {
		return enabledFilters.get( filterName );
	}

	public Filter enableFilter(String filterName) {
		FilterImpl filter = new FilterImpl( sessionFactory.getFilterDefinition( filterName ) );
		enabledFilters.put( filterName, filter );
		return filter;
	}

	public void disableFilter(String filterName) {
		enabledFilters.remove( filterName );
	}

	public Object getFilterParameterValue(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
		final FilterImpl filter = (FilterImpl) enabledFilters.get( parsed[0] );
		if ( filter == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		return filter.getParameter( parsed[1] );
	}

	public Type getFilterParameterType(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
		final FilterDefinition filterDef = sessionFactory.getFilterDefinition( parsed[0] );
		if ( filterDef == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] not defined" );
		}
		final Type type = filterDef.getParameterType( parsed[1] );
		if ( type == null ) {
			// this is an internal error of some sort...
			throw new InternalError( "Unable to locate type for filter parameter" );
		}
		return type;
	}

	public static String[] parseFilterParameterName(String filterParameterName) {
		int dot = filterParameterName.lastIndexOf( '.' );
		if ( dot <= 0 ) {
			throw new IllegalArgumentException(
					"Invalid filter-parameter name format [" + filterParameterName + "]; expecting {filter-name}.{param-name}"
			);
		}
		final String filterName = filterParameterName.substring( 0, dot );
		final String parameterName = filterParameterName.substring( dot + 1 );
		return new String[] { filterName, parameterName };
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return !enabledFetchProfileNames.isEmpty();
	}

	public Set<String> getEnabledFetchProfileNames() {
		return enabledFetchProfileNames;
	}

	private void checkFetchProfileName(String name) {
		if ( !sessionFactory.containsFetchProfileDefinition( name ) ) {
			throw new UnknownProfileException( name );
		}
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		return enabledFetchProfileNames.contains( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		enabledFetchProfileNames.add( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		enabledFetchProfileNames.remove( name );
	}

	public EffectiveEntityGraph getEffectiveEntityGraph() {
		return effectiveEntityGraph;
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	@Remove
	public EntityGraph getFetchGraph() {
		if ( effectiveEntityGraph.getSemantic() != GraphSemantic.FETCH ) {
			return null;
		}

		return effectiveEntityGraph.getGraph();
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	@Remove
	public void setFetchGraph(EntityGraph fetchGraph) {
		effectiveEntityGraph.applyGraph( (RootGraphImplementor<?>) fetchGraph, GraphSemantic.FETCH );
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	@Remove
	public EntityGraph getLoadGraph() {
		if ( effectiveEntityGraph.getSemantic() != GraphSemantic.LOAD ) {
			return null;
		}

		return effectiveEntityGraph.getGraph();
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	@Remove
	public void setLoadGraph(final EntityGraph loadGraph) {
		effectiveEntityGraph.applyGraph( (RootGraphImplementor<?>) loadGraph, GraphSemantic.LOAD );
	}

}
