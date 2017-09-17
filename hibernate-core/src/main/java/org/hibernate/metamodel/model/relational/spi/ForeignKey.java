/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.List;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class ForeignKey implements Exportable {
	private final String name;
	private final Table referringTable;
	private final Table targetTable;
	private final ColumnMappings columnMappings;
	private final boolean cascadeDeleteEnabled;
	private final boolean export;
	private final String keyDefinition;

	public ForeignKey(
			String name,
			boolean export,
			String keyDefinition,
			boolean cascadeDeleteEnabled,
			Table referringTable,
			Table targetTable,
			ColumnMappings columnMappings) {
		this.name = name;
		this.export = export;
		this.keyDefinition = keyDefinition;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.referringTable = referringTable;
		this.targetTable = targetTable;
		this.columnMappings = columnMappings;
	}

	public String getName() {
		return name;
	}

	public Table getReferringTable() {
		return referringTable;
	}

	public Table getTargetTable() {
		return targetTable;
	}

	public ColumnMappings getColumnMappings() {
		return columnMappings;
	}

	public boolean isExportationEnabled() {
		return export;
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( ( (ExportableTable) referringTable ).getTableName().getText(), "FK-" + getName() );
	}

	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	public boolean isReferenceToPrimaryKey() {
		return getColumnMappings().getTargetColumns().isEmpty();
	}

	public String getKeyDefinition() {
		return keyDefinition;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	/**
	 * Models the metadata for a foreign key (physical or logical).
	 * <p/>
	 * The terms "target" and "referring" used here are relative to the
	 * direction of the FK.  E.g. given a table {@code ADDRESS} that defines a foreign key
	 * referring to a {@code PERSON} table... {@code ADDRESS} would be the referencing
	 * table and {@code PERSON} would be the target table.  The columns in {@code ADDRESS}
	 * making up the FK would be the referring columns, and the columns on the {@code PERSON}
	 * table (referred to by the FK) would be the target columns:
	 * <pre>{@code
	 *     referring                target
	 *     -----------------        ----------------------------
	 *     ADDRESS             ->   PERSON
	 *         PERSON_ID       ->       ID
	 *     }
	 * </pre>
	 * <p/>
	 * Note that this is made an interface to allow for variants of FK declaration to
	 * potentially provide dedicated impls.  These variants include:<ul>
	 *     <li>FKs that reference to the target table's PK and that name the columns the same</li>
	 *     <li>FKs that reference to the target table's PK and define them in the same order</li>
	 *     <li>FKs that reference to column(s) in the target table that are not its defined PK</li>
	 * </ul>
	 * <p/>
	 * Note also that it is expected that the ColumnMappings have been validated prior to passing the in:<ul>
	 *     <li>both sides define the same number of Columns</li>
	 *     <li>
	 *         all referring Columns come from the referring Table and that all target Columns come from the
	 *         target Table as defined by {@link Column#getSourceTable()}
	 *     </li>
	 * </ul>
	 */
	public interface ColumnMappings {
		Table getReferringTable();
		Table getTargetTable();
		List<ColumnMapping> getColumnMappings();
		List<PhysicalColumn> getTargetColumns();
		List<PhysicalColumn> getReferringColumns();

		// todo (6.0) : consider exposing org.hibernate.type.ForeignKeyDirection here
	}

	public interface ColumnMapping {
		PhysicalColumn getReferringColumn();
		PhysicalColumn getTargetColumn();
	}
}