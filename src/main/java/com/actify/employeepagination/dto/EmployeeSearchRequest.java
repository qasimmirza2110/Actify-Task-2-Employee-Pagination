package com.actify.employeepagination.dto;

import java.math.BigDecimal;

/**
 * Query parameters of GET /api/employees after the controller has read them.
 * sort has the form "field" or "field,direction", for example "salary,desc".
 */
public record EmployeeSearchRequest(String department, String name, BigDecimal minSalary, BigDecimal maxSalary,
		int page, int size, String sort) {
}
