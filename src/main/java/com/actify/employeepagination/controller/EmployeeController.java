package com.actify.employeepagination.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actify.employeepagination.dto.EmployeeResponse;
import com.actify.employeepagination.dto.EmployeeSearchRequest;
import com.actify.employeepagination.dto.PageResponse;
import com.actify.employeepagination.service.EmployeeService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Public endpoint (no login). Example:
 * GET /api/employees?page=0&size=10&sort=salary,desc&department=IT&minSalary=30000&name=qasim
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	public PageResponse<EmployeeResponse> getEmployees(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			// The upper limit (app.employee.max-page-size) is checked in EmployeeService.
			@RequestParam(defaultValue = "10") @Min(1) int size,
			@RequestParam(defaultValue = "id,asc") String sort,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) @PositiveOrZero BigDecimal minSalary,
			@RequestParam(required = false) @PositiveOrZero BigDecimal maxSalary) {
		return employeeService.searchEmployees(
				new EmployeeSearchRequest(department, name, minSalary, maxSalary, page, size, sort));
	}
}
