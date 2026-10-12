package com.actify.employeepagination;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.actify.employeepagination.entity.Employee;
import com.actify.employeepagination.repository.EmployeeRepository;

/**
 * Full application + real PostgreSQL (financial_ledger_task2_test_db). The endpoint is public, so no token is sent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeIT {

	/**
	 * Fail-closed guard: these tests delete rows and Hibernate drops/creates the schema (create-drop), so refuse to
	 * start unless the connection really points at the dedicated local test database. Runs before Hibernate uses it.
	 */
	@TestConfiguration
	static class TestDatabaseGuard {

		static final String EXPECTED_DATABASE = "financial_ledger_task2_test_db";

		@Bean
		static BeanPostProcessor testDatabaseGuard() {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					if (bean instanceof DataSource dataSource) {
						verify(dataSource);
					}
					return bean;
				}
			};
		}

		private static void verify(DataSource dataSource) {
			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement();
					ResultSet result = statement.executeQuery("SELECT current_database()")) {
				result.next();
				String database = result.getString(1);
				String host = URI.create(connection.getMetaData().getURL().substring("jdbc:".length())).getHost();
				if (!EXPECTED_DATABASE.equals(database) || !List.of("localhost", "127.0.0.1", "[::1]").contains(host)) {
					throw new IllegalStateException("Refusing to run integration tests against database '" + database
							+ "' on host '" + host + "'. Expected " + EXPECTED_DATABASE + " on localhost.");
				}
			} catch (SQLException e) {
				throw new IllegalStateException("Could not verify the test database", e);
			}
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeRepository employeeRepository;

	@BeforeEach
	void setUp() {
		employeeRepository.deleteAll();
		employeeRepository.saveAll(List.of(
				new Employee("Qasim", "IT", new BigDecimal("50000.00")),
				new Employee("Sakshi", "HR", new BigDecimal("45000.00")),
				new Employee("Abbas", "Finance", new BigDecimal("80000.00")),
				new Employee("Ali", "IT", new BigDecimal("90000.00")),
				new Employee("Nupur", "IT", new BigDecimal("30000.00")),
				new Employee("Sahil", "HR", new BigDecimal("60000.00")),
				new Employee("Aakash", "Finance", new BigDecimal("25000.00"))));
	}

	private ResultActions getEmployees(String query) throws Exception {
		return mockMvc.perform(get("/api/employees" + query));
	}

	@Test
	void responseHasPageStructureAndDefaultsToPageZeroSizeTenSortedById() throws Exception {
		getEmployees("")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPage").value(0))
				.andExpect(jsonPath("$.pageSize").value(10))
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.content.length()").value(7))
				.andExpect(jsonPath("$.content[0].id").isNumber())
				.andExpect(jsonPath("$.content[0].name").value("Qasim"))
				.andExpect(jsonPath("$.content[0].department").value("IT"))
				.andExpect(jsonPath("$.content[0].salary").value(50000.00))
				.andExpect(jsonPath("$.content[6].name").value("Aakash"));
	}

	@Test
	void requestedPageAndSize() throws Exception {
		getEmployees("?page=1&size=3")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPage").value(1))
				.andExpect(jsonPath("$.pageSize").value(3))
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(3))
				.andExpect(jsonPath("$.content[*].name", contains("Ali", "Nupur", "Sahil")));
		getEmployees("?page=2&size=3")
				.andExpect(jsonPath("$.content[*].name", contains("Aakash")));
	}

	@Test
	void pageAfterTheLastOneIsEmptyButCountStaysCorrect() throws Exception {
		getEmployees("?page=5&size=3")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(3));
	}

	@Test
	void pageSizeLimits() throws Exception {
		getEmployees("?size=100").andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(100));
		getEmployees("?size=101")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
		getEmployees("?size=0").andExpect(status().isBadRequest());
		getEmployees("?page=-1").andExpect(status().isBadRequest());
		getEmployees("?page=abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value for parameter 'page'"));
	}

	@Test
	void sortAscendingAndDescending() throws Exception {
		getEmployees("?sort=salary,asc")
				.andExpect(jsonPath("$.content[*].name", contains("Aakash", "Nupur", "Sakshi",
						"Qasim", "Sahil", "Abbas", "Ali")));
		getEmployees("?sort=salary,desc")
				.andExpect(jsonPath("$.content[0].name").value("Ali"))
				.andExpect(jsonPath("$.content[6].name").value("Aakash"));
		getEmployees("?sort=name")
				.andExpect(jsonPath("$.content[*].name", contains("Aakash", "Abbas", "Ali", "Nupur",
						"Qasim", "Sahil", "Sakshi")));
		// Same department: id is the tie-breaker, so the order is always the same.
		getEmployees("?sort=department,desc")
				.andExpect(jsonPath("$.content[*].name", contains("Qasim", "Ali", "Nupur", "Sakshi",
						"Sahil", "Abbas", "Aakash")));
	}

	@Test
	void invalidSortIsRejectedWithClearMessage() throws Exception {
		getEmployees("?sort=passwordHash,asc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/employees"))
				.andExpect(jsonPath("$.message")
						.value("Invalid sort field 'passwordHash'. Allowed: id, name, department, salary"));
		getEmployees("?sort=salary,up").andExpect(status().isBadRequest());
	}

	@Test
	void departmentFilterIsExactAndCaseInsensitive() throws Exception {
		getEmployees("?department=it")
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[*].name", contains("Qasim", "Ali", "Nupur")));
		// Exact match: "I" is not a department.
		getEmployees("?department=I").andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void minAndMaxSalaryAreInclusive() throws Exception {
		getEmployees("?minSalary=60000")
				.andExpect(jsonPath("$.content[*].name", contains("Abbas", "Ali", "Sahil")));
		getEmployees("?maxSalary=45000")
				.andExpect(jsonPath("$.content[*].name", contains("Sakshi", "Nupur", "Aakash")));
		getEmployees("?minSalary=45000&maxSalary=60000")
				.andExpect(jsonPath("$.content[*].name", contains("Qasim", "Sakshi", "Sahil")));
	}

	@Test
	void invalidSalaryValuesAreRejected() throws Exception {
		getEmployees("?minSalary=80000&maxSalary=30000")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("minSalary must not be greater than maxSalary"));
		getEmployees("?minSalary=-1").andExpect(status().isBadRequest());
		getEmployees("?maxSalary=lots").andExpect(status().isBadRequest());
	}

	@Test
	void partialNameSearchIsCaseInsensitive() throws Exception {
		getEmployees("?name=AS")
				.andExpect(jsonPath("$.content[*].name", contains("Qasim", "Abbas", "Aakash")));
		getEmployees("?name=as")
				.andExpect(jsonPath("$.content[*].name", contains("Qasim", "Abbas", "Aakash")));
		getEmployees("?name=zzz")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0))
				.andExpect(jsonPath("$.totalPages").value(0))
				.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void combinedFiltersWithPagingAndSorting() throws Exception {
		getEmployees("?department=IT&minSalary=40000&name=a&sort=salary,desc&page=0&size=1")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2)) // Ali 90000, Qasim 50000
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.content[*].name", contains("Ali")));
		getEmployees("?department=IT&minSalary=40000&name=a&sort=salary,desc&page=1&size=1")
				.andExpect(jsonPath("$.content[*].name", contains("Qasim")));
		getEmployees("?department=finance&name=AS&maxSalary=50000")
				.andExpect(jsonPath("$.content[*].name", contains("Aakash")));
	}

	@Test
	void unknownUrlReturnsJson404() throws Exception {
		mockMvc.perform(get("/api/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}
}
