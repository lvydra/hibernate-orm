/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.JavaType;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = CharacterArrayMappingTests.EntityWithCharArrays.class)
@SessionFactory
public class CharacterArrayMappingTests {
	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor(EntityWithCharArrays.class);

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			if ( dialect.supportsStandardArrays() ) {
				assertThat( jdbcMapping.getJdbcType(), instanceOf( ArrayJdbcType.class ) );
				assertThat(
						( (ArrayJdbcType) jdbcMapping.getJdbcType() ).getElementJdbcType(),
						is( jdbcRegistry.getDescriptor( Types.CHAR ) )
				);
			}
			else {
				assertThat(
						jdbcMapping.getJdbcType(),
						isOneOf(
								jdbcRegistry.getDescriptor( SqlTypes.ARRAY ),
								jdbcRegistry.getDescriptor( SqlTypes.SQLXML )
						)
				);
			}
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperOld");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.CLOB)));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.CLOB)));
		}
	}

	@Entity(name = "EntityWithCharArrays")
	@Table(name = "EntityWithCharArrays")
	public static class EntityWithCharArrays {
		@Id
		public Integer id;

		//tag::basic-chararray-example[]
		// mapped as VARCHAR
		char[] primitive;
		Character[] wrapper;
		@JavaType( CharacterArrayJavaType.class )
		Character[] wrapperOld;

		// mapped as CLOB
		@Lob
		char[] primitiveClob;
		@Lob
		@JavaType( CharacterArrayJavaType.class )
		Character[] wrapperClob;
		//end::basic-chararray-example[]
	}
}
