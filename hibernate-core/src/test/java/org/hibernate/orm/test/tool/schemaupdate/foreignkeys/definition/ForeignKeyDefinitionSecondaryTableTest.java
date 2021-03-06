/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys.definition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.junit5.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(dialectClass = H2Dialect.class, matchSubTypes = true)
public class ForeignKeyDefinitionSecondaryTableTest
		extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				User.class,
		};
	}

	@Entity(name = "Users")
	@SecondaryTable(name = "User_details", foreignKey = @ForeignKey(name = "secondary", foreignKeyDefinition = "foreign key /* FK */ (id) references Users"))
	public class User {

		@Id
		@GeneratedValue
		private int id;

		private String emailAddress;

		@Column(name = "SECURITY_USERNAME", table = "User_details")
		private String username;

		@Column(name = "SECURITY_PASSWORD", table = "User_details")
		private String password;
	}


	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* FK */" );
	}
}
