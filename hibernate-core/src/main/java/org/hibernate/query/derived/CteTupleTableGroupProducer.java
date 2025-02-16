/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.type.BasicType;

/**
 * The table group producer for a CTE tuple type.
 *
 * Exposes additional access to some special model parts for recursive CTE attributes.
 *
 * @author Christian Beikov
 */
@Incubating
public class CteTupleTableGroupProducer extends AnonymousTupleTableGroupProducer {

	private final AnonymousTupleBasicValuedModelPart searchModelPart;
	private final AnonymousTupleBasicValuedModelPart cycleMarkModelPart;
	private final AnonymousTupleBasicValuedModelPart cyclePathModelPart;

	public CteTupleTableGroupProducer(
			SqmCteTable<?> sqmCteTable,
			String aliasStem,
			List<SqlSelection> sqlSelections,
			FromClauseAccess fromClauseAccess) {
		super( sqmCteTable, aliasStem, sqlSelections, fromClauseAccess );
		final SqmCteStatement<?> cteStatement = sqmCteTable.getCteStatement();
		final BasicType<String> stringType = cteStatement.nodeBuilder()
				.getTypeConfiguration()
				.getBasicTypeForJavaType( String.class );
		this.searchModelPart = createModelPart( cteStatement.getSearchAttributeName(), stringType );
		this.cycleMarkModelPart = createModelPart(
				cteStatement.getCycleMarkAttributeName(),
				cteStatement.getCycleLiteral() == null
						? null
						: (BasicType<?>) cteStatement.getCycleLiteral().getNodeType()
		);
		this.cyclePathModelPart = createModelPart( cteStatement.getCyclePathAttributeName(), stringType );
	}

	private static AnonymousTupleBasicValuedModelPart createModelPart(String attributeName, BasicType<?> basicType) {
		if ( attributeName != null ) {
			return new AnonymousTupleBasicValuedModelPart(
					attributeName,
					attributeName,
					basicType,
					basicType,
					-1
			);
		}
		return null;
	}

	public List<CteColumn> determineCteColumns() {
		final List<CteColumn> columns = new ArrayList<>( getModelParts().size() );
		forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					columns.add(
							new CteColumn(
									selectableMapping.getSelectionExpression(),
									selectableMapping.getJdbcMapping()
							)
					);
				}
		);
		return columns;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		final ModelPart subPart = super.findSubPart( name, treatTargetType );
		if ( subPart != null ) {
			return subPart;
		}
		if ( searchModelPart != null && name.equals( searchModelPart.getPartName() ) ) {
			return searchModelPart;
		}
		if ( cycleMarkModelPart != null && name.equals( cycleMarkModelPart.getPartName() ) ) {
			return cycleMarkModelPart;
		}
		if ( cyclePathModelPart != null && name.equals( cyclePathModelPart.getPartName() ) ) {
			return cyclePathModelPart;
		}
		return null;
	}

}
