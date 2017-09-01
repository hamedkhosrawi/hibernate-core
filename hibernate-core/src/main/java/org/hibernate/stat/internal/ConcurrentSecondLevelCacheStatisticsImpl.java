/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.SecondLevelCacheStatistics;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class ConcurrentSecondLevelCacheStatisticsImpl extends CategorizedStatistics implements SecondLevelCacheStatistics {
	private final transient Region region;

	private AtomicLong hitCount = new AtomicLong();
	private AtomicLong missCount = new AtomicLong();
	private AtomicLong putCount = new AtomicLong();

	ConcurrentSecondLevelCacheStatisticsImpl(Region region) {
		super( region.getName() );
		this.region = region;
	}

	@Override
	public long getHitCount() {
		return hitCount.get();
	}

	@Override
	public long getMissCount() {
		return missCount.get();
	}

	@Override
	public long getPutCount() {
		return putCount.get();
	}

	@Override
	public long getElementCountInMemory() {
		return ExtendedStatisticsSupport.class.isInstance( region )
				? ( (ExtendedStatisticsSupport) region ).getElementCountInMemory()
				: Long.MIN_VALUE;
	}

	@Override
	public long getElementCountOnDisk() {
		return ExtendedStatisticsSupport.class.isInstance( region )
				? ( (ExtendedStatisticsSupport) region ).getElementCountOnDisk()
				: Long.MIN_VALUE;
	}

	@Override
	public long getSizeInMemory() {
		return ExtendedStatisticsSupport.class.isInstance( region )
				? ( (ExtendedStatisticsSupport) region ).getSizeInMemory()
				: Long.MIN_VALUE;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder()
				.append( "SecondLevelCacheStatistics" )
				.append( "[hitCount=").append( this.hitCount )
				.append( ",missCount=").append( this.missCount )
				.append( ",putCount=").append( this.putCount );
		// not sure if this would ever be null but wanted to be careful
		if ( region != null ) {
			buf.append( ",elementCountInMemory=" ).append( this.getElementCountInMemory() )
					.append( ",elementCountOnDisk=" ).append( this.getElementCountOnDisk() )
					.append( ",sizeInMemory=" ).append( this.getSizeInMemory() );
		}
		buf.append( ']' );
		return buf.toString();
	}

	void incrementHitCount() {
		hitCount.getAndIncrement();
	}

	void incrementMissCount() {
		missCount.getAndIncrement();
	}

	void incrementPutCount() {
		putCount.getAndIncrement();
	}
}
