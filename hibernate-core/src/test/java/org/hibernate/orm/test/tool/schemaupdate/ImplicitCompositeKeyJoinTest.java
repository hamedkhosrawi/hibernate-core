/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import org.apache.log4j.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9865")
public class ImplicitCompositeKeyJoinTest extends BaseSchemaUnitTestCase {
	private static final Logger LOGGER = Logger.getLogger( ImplicitCompositeKeyJoinTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testSchemaCreationSQLCommandIsGeneratedWithTheCorrectColumnSizeValues(SchemaScope scope) {
		scope.withSchemaCreator( null, schemaCreator -> {
			boolean createTableEmployeeFound = false;
			final List<String> commands = schemaCreator.generateCreationCommands(
					getMetadata(),
					false
			);
			for ( String command : commands ) {
				LOGGER.info( command );
				if ( command.toLowerCase().contains( "create table employee" ) ) {
					final String[] columnsDefinition = getColumnsDefinition( command );

					for ( int i = 0; i < columnsDefinition.length; i++ ) {
						checkColumnSize( columnsDefinition[i] );
					}
					createTableEmployeeFound = true;
				}
			}
			assertTrue(
					createTableEmployeeFound,
					"Expected create table command for Employee entity not found"
			);
		} );
	}

	private String[] getColumnsDefinition(String command) {
		String substring = command.toLowerCase().replaceAll( "create table employee ", "" );
		substring = substring.substring( 0, substring.toLowerCase().indexOf( "primary key" ) );
		return substring.split( "\\," );
	}

	private void checkColumnSize(String s) {
		if ( s.toLowerCase().contains( "manager_age" ) ) {
			if ( !s.contains( "15" ) ) {
				fail( expectedMessage( "manager_age", 15, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "manager_birthday" ) ) {
			if ( !s.contains( "255" ) ) {
				fail( expectedMessage( "manager_birthday", 255, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "manager_name" ) ) {
			if ( !s.contains( "20" ) ) {
				fail( expectedMessage( "manager_name", 20, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "age" ) ) {
			if ( !s.contains( "15" ) ) {
				fail( expectedMessage( "age", 15, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "birthday" ) ) {
			if ( !s.contains( "255" ) ) {
				fail( expectedMessage( "birthday", 255, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "name" ) ) {
			if ( !s.contains( "20" ) ) {
				fail( expectedMessage( "name", 20, s ) );
			}
		}
	}

	private String expectedMessage(String column_name, int size, String actual) {
		return "Expected " + column_name + " " + size + " but was " + actual;
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@EmbeddedId
		@ForeignKey(name = "none")
		private EmployeeId id;

		@ManyToOne(optional = true)
		@ForeignKey(name = "none")
		private Employee manager;
	}

	@Embeddable
	public class EmployeeId implements Serializable {
		@Column(length = 15)
		public String age;

		@Column(length = 20)
		private String name;

		private String birthday;
	}
}
