/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = NativeQueryCacheWithExtraColumnsTest.TestUser.class)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
		}
)
public class NativeQueryCacheWithExtraColumnsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Add extra columns to the table that are not mapped in the entity
			session.createNativeMutationQuery( "ALTER TABLE TEST_USER ADD COLUMN IF NOT EXISTS EXTRA_COL1 VARCHAR(50)" ).executeUpdate();
			session.createNativeMutationQuery( "ALTER TABLE TEST_USER ADD COLUMN IF NOT EXISTS EXTRA_COL2 VARCHAR(50)" ).executeUpdate();

			// Insert test data with extra columns
			session.createNativeMutationQuery(
					"INSERT INTO TEST_USER (ID, NAME, EMAIL, AGE, ADDRESS, PHONE, EXTRA_COL1, EXTRA_COL2) VALUES " +
							"(1, 'john', 'john@test.com', 30, 'ny', '123456', 'ext1', 'ext2')"
			).executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from TestUser" ).executeUpdate();
		} );
	}

	@Test
	public void testNativeQueryWithCacheAndExtraColumns(SessionFactoryScope scope) {
		// First query - should populate the cache
		scope.inTransaction( session -> {
			NativeQuery<TestUser> query = session.createNativeQuery(
					"SELECT u1.* FROM TEST_USER u1, TEST_USER u2 WHERE u2.ID = u1.ID",
					TestUser.class
			);
			query.setCacheable( true );

			List<TestUser> users = query.getResultList();
			assertThat( users ).hasSize( 1 );
			assertThat( users.get( 0 ).getName() ).isEqualTo( "john" );
		} );

		// Second query - should use the cache
		// This is where ArrayIndexOutOfBoundsException may occur
		scope.inTransaction( session -> {
			NativeQuery<TestUser> query = session.createNativeQuery(
					"SELECT u1.* FROM TEST_USER u1, TEST_USER u2 WHERE u2.ID = u1.ID",
					TestUser.class
			);
			query.setCacheable( true );

			List<TestUser> users = query.getResultList();
			assertThat( users ).hasSize( 1 );
			assertThat( users.get( 0 ).getName() ).isEqualTo( "john" );
			assertThat( users.get( 0 ).getEmail() ).isEqualTo( "john@test.com" );
			assertThat( users.get( 0 ).getAge() ).isEqualTo( 30 );
		} );
	}

	@Test
	public void testNativeQueryWithoutCacheAndExtraColumns(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NativeQuery<TestUser> query = session.createNativeQuery(
					"SELECT u1.* FROM TEST_USER u1, TEST_USER u2 WHERE u2.ID = u1.ID",
					TestUser.class
			);
			query.setCacheable( false );

			List<TestUser> users = query.getResultList();
			assertThat( users ).hasSize( 1 );
			assertThat( users.get( 0 ).getName() ).isEqualTo( "john" );
			assertThat( users.get( 0 ).getEmail() ).isEqualTo( "john@test.com" );
			assertThat( users.get( 0 ).getAge() ).isEqualTo( 30 );
			assertThat( users.get( 0 ).getAddress() ).isEqualTo( "ny" );
			assertThat( users.get( 0 ).getPhone() ).isEqualTo( "123456" );
		} );

		// Execute again to verify it works consistently without cache
		scope.inTransaction( session -> {
			NativeQuery<TestUser> query = session.createNativeQuery(
					"SELECT u1.* FROM TEST_USER u1, TEST_USER u2 WHERE u2.ID = u1.ID",
					TestUser.class
			);
			query.setCacheable( false );

			List<TestUser> users = query.getResultList();
			assertThat( users ).hasSize( 1 );
			assertThat( users.get( 0 ).getName() ).isEqualTo( "john" );
		} );
	}

	@Entity(name = "TestUser")
	@Table(name = "TEST_USER")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestUser {

		@Id
		@Column(name = "ID")
		private Long id;

		@Column(name = "NAME")
		private String name;

		@Column(name = "EMAIL")
		private String email;

		@Column(name = "AGE")
		private Integer age;

		@Column(name = "ADDRESS")
		private String address;

		@Column(name = "PHONE")
		private String phone;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}
	}
}
