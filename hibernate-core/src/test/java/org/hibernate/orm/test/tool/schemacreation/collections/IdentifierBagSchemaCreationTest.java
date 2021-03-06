/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.collections;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class IdentifierBagSchemaCreationTest extends BaseSchemaCreationTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@SchemaTest
	public void testElementAndCollectionTableAreCreated(SchemaScope scope) {
		assertThatTablesAreCreated(
				"item (id bigint not null, primary key (id))",
				"image (image_id bigint not null, image_name varchar(255), item_id bigint not null, primary key (image_id))"
		);

		assertThatActionIsGenerated(
				"alter table image add constraint (.*) foreign key \\(item_id\\) references item"
		);
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	@GenericGenerator(name = "increment", strategy = "increment")
	public static class Item {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(name = "IMAGE", joinColumns = @JoinColumn(name = "ITEM_ID"))
		@Column(name = "IMAGE_NAME")
		@CollectionId(columns = @Column(name = "IMAGE_ID"),
				type = @Type(type = "long"), generator = "increment")
		private Collection<String> images = new ArrayList<>();
	}
}
