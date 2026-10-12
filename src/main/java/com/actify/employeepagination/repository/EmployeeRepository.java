package com.actify.employeepagination.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.actify.employeepagination.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	/**
	 * One query for every filter combination: a null filter is ignored. The count query must have the same
	 * WHERE clause, otherwise totalElements and totalPages would not match the filters.
	 *
	 * The service lower-cases department and name before calling this. LOWER(:department) in the query fails on
	 * PostgreSQL when the value is null ("function lower(bytea) does not exist").
	 */
	@Query(value = """
			SELECT e FROM Employee e
			WHERE (:department IS NULL OR LOWER(e.department) = :department)
			  AND (:minSalary IS NULL OR e.salary >= :minSalary)
			  AND (:maxSalary IS NULL OR e.salary <= :maxSalary)
			  AND (:namePattern IS NULL OR LOWER(e.name) LIKE :namePattern)
			""",
			countQuery = """
			SELECT COUNT(e) FROM Employee e
			WHERE (:department IS NULL OR LOWER(e.department) = :department)
			  AND (:minSalary IS NULL OR e.salary >= :minSalary)
			  AND (:maxSalary IS NULL OR e.salary <= :maxSalary)
			  AND (:namePattern IS NULL OR LOWER(e.name) LIKE :namePattern)
			""")
	Page<Employee> search(@Param("department") String department, @Param("minSalary") BigDecimal minSalary,
			@Param("maxSalary") BigDecimal maxSalary, @Param("namePattern") String namePattern, Pageable pageable);
}
