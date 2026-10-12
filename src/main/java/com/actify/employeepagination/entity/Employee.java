package com.actify.employeepagination.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees", indexes = {
		@Index(name = "idx_employees_department", columnList = "department"),
		@Index(name = "idx_employees_salary", columnList = "salary") })
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 50)
	private String department;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal salary;

	protected Employee() {
	}

	public Employee(String name, String department, BigDecimal salary) {
		this.name = name;
		this.department = department;
		this.salary = salary;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDepartment() {
		return department;
	}

	public BigDecimal getSalary() {
		return salary;
	}
}
