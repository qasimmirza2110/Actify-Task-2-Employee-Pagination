package com.actify.employeepagination.dto;

import java.math.BigDecimal;

import com.actify.employeepagination.entity.Employee;

public record EmployeeResponse(Long id, String name, String department, BigDecimal salary) {

	public static EmployeeResponse from(Employee employee) {
		return new EmployeeResponse(employee.getId(), employee.getName(), employee.getDepartment(),
				employee.getSalary());
	}
}
