package com.actify.employeepagination.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actify.employeepagination.dto.EmployeeResponse;
import com.actify.employeepagination.dto.EmployeeSearchRequest;
import com.actify.employeepagination.dto.PageResponse;
import com.actify.employeepagination.exception.InvalidPageRequestException;
import com.actify.employeepagination.exception.InvalidSalaryRangeException;
import com.actify.employeepagination.repository.EmployeeRepository;

@Service
public class EmployeeService {

	// Only these fields may be used for sorting. Anything else is rejected instead of being passed
	// to the database query.
	private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "department", "salary");

	private final EmployeeRepository employeeRepository;
	private final int maxPageSize;

	public EmployeeService(EmployeeRepository employeeRepository,
			@Value("${app.employee.max-page-size:100}") int maxPageSize) {
		this.employeeRepository = employeeRepository;
		this.maxPageSize = maxPageSize;
	}

	@Transactional(readOnly = true)
	public PageResponse<EmployeeResponse> searchEmployees(EmployeeSearchRequest request) {
		if (request.minSalary() != null && request.maxSalary() != null
				&& request.minSalary().compareTo(request.maxSalary()) > 0) {
			throw new InvalidSalaryRangeException("minSalary must not be greater than maxSalary");
		}
		Pageable pageable = buildPageable(request);
		String department = lowerCaseOrNull(request.department());
		String name = lowerCaseOrNull(request.name());
		String namePattern = name == null ? null : "%" + name + "%";
		return PageResponse.from(employeeRepository.search(department, request.minSalary(), request.maxSalary(),
				namePattern, pageable), EmployeeResponse::from);
	}

	Pageable buildPageable(EmployeeSearchRequest request) {
		if (request.page() < 0) {
			throw new InvalidPageRequestException("page must be 0 or greater");
		}
		if (request.size() < 1 || request.size() > maxPageSize) {
			throw new InvalidPageRequestException("size must be between 1 and " + maxPageSize);
		}
		return PageRequest.of(request.page(), request.size(), parseSort(request.sort()));
	}

	Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.ASC, "id");
		}
		String[] parts = sort.split(",");
		if (parts.length > 2) {
			throw new InvalidPageRequestException("sort must look like 'field' or 'field,asc|desc'");
		}
		String field = parts[0].trim();
		if (!ALLOWED_SORT_FIELDS.contains(field)) {
			throw new InvalidPageRequestException(
					"Invalid sort field '" + field + "'. Allowed: id, name, department, salary");
		}
		Sort.Direction direction = Sort.Direction.ASC;
		if (parts.length == 2) {
			String value = parts[1].trim().toLowerCase(Locale.ROOT);
			if (!value.equals("asc") && !value.equals("desc")) {
				throw new InvalidPageRequestException("Sort direction must be 'asc' or 'desc'");
			}
			direction = Sort.Direction.fromString(value);
		}
		// id as a second sort key keeps the order stable when many rows have the same value.
		Sort result = Sort.by(direction, field);
		return field.equals("id") ? result : result.and(Sort.by(Sort.Direction.ASC, "id"));
	}

	private String lowerCaseOrNull(String value) {
		return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
	}
}
