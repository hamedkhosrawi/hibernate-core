/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Gail Badner
 */
public class MySQL57Dialect extends MySQL55Dialect {
	public MySQL57Dialect() {
		super();

		// For details about MySQL 5.7 support for fractional seconds
		// precision (fsp): http://dev.mysql.com/doc/refman/5.7/en/fractional-seconds.html
		// Regarding datetime(fsp), "The fsp value, if given, must be
		// in the range 0 to 6. A value of 0 signifies that there is
		// no fractional part. If omitted, the default precision is 0.
		// (This differs from the standard SQL default of 6, for
		// compatibility with previous MySQL versions.)".

		// The following is defined because Hibernate currently expects
		// the SQL 1992 default of 6 (which is inconsistent with the MySQL
		// default).
		registerColumnType( Types.TIMESTAMP, "datetime(6)" );

		// MySQL 5.7 brings JSON native support with a dedicated datatype.
		// For more details about MySql new JSON datatype support, see:
		// https://dev.mysql.com/doc/refman/5.7/en/json.html
		registerColumnType( Types.JAVA_OBJECT, "json" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );
		// MySQL also supports fractional seconds precision for time values
		// (time(fsp)). According to SQL 1992, the default for <time precision>
		// is 0. The MySQL default is time(0), there's no need to override
		// the setting for Types.TIME columns.

		// For details about MySQL support for timestamp functions, see:
		// http://dev.mysql.com/doc/refman/5.7/en/date-and-time-functions.html

		// The following are synonyms for now(fsp), where fsp defaults to 0 on MySQL 5.7:
		// current_timestamp([fsp]), localtime(fsp), localtimestamp(fsp).
		// Register the same StaticPrecisionFspTimestampFunction for all 4 functions.
		registry.registerNoArgs( "now", "now(6)", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "current_timestamp", "now(6)", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "localtime", "now(6)", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "localtimestamp", "now(6)", StandardSpiBasicTypes.TIMESTAMP );

		// sysdate is different from now():
		// "SYSDATE() returns the time at which it executes. This differs
		// from the behavior for NOW(), which returns a constant time that
		// indicates the time at which the statement began to execute.
		// (Within a stored function or trigger, NOW() returns the time at
		// which the function or triggering statement began to execute.)
		registry.registerNoArgs( "sysdate", "sysdate(6)", StandardSpiBasicTypes.TIMESTAMP );

		// from_unixtime(), timestamp() are functions that return TIMESTAMP that do not support a
		// fractional seconds precision argument (so there's no need to override them here):
	}

	/**
	 * @see <a href="https://dev.mysql.com/worklog/task/?id=7019">MySQL 5.7 work log</a>
	 * @return supports IN clause row value expressions
	 */
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}
}
