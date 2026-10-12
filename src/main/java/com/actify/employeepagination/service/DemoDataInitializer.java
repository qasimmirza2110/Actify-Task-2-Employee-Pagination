package com.actify.employeepagination.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.actify.employeepagination.entity.Employee;
import com.actify.employeepagination.repository.EmployeeRepository;

/**
 * The task only asks for a read API, so there is no endpoint to add employees.
 * For a local demo this inserts sample rows, but only when app.seed-demo-data=true and the table is empty.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

	private final EmployeeRepository employeeRepository;
	private final boolean seedDemoData;

	public DemoDataInitializer(EmployeeRepository employeeRepository,
			@Value("${app.seed-demo-data:false}") boolean seedDemoData) {
		this.employeeRepository = employeeRepository;
		this.seedDemoData = seedDemoData;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!seedDemoData || employeeRepository.count() > 0) {
			return;
		}
		employeeRepository.saveAll(List.of(
				new Employee("Qasim", "IT", new BigDecimal("50000.00")),
				new Employee("Sakshi", "HR", new BigDecimal("45000.00")),
				new Employee("Abbas", "Finance", new BigDecimal("80000.00")),
				new Employee("Ali", "IT", new BigDecimal("90000.00")),
				new Employee("Nupur", "IT", new BigDecimal("30000.00")),
				new Employee("Sahil", "HR", new BigDecimal("60000.00")),
				new Employee("Aakash", "Finance", new BigDecimal("25000.00"))));
		log.info("Inserted 7 demo employees");
	}
}
