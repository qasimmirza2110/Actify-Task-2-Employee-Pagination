package com.actify.employeepagination.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.actify.employeepagination.dto.EmployeeSearchRequest;
import com.actify.employeepagination.exception.InvalidPageRequestException;
import com.actify.employeepagination.exception.InvalidSalaryRangeException;
import com.actify.employeepagination.repository.EmployeeRepository;

class EmployeeServiceTest {

	private final EmployeeRepository repository = mock(EmployeeRepository.class);
	private final EmployeeService service = new EmployeeService(repository, 100);

	private EmployeeSearchRequest request(int page, int size, String sort) {
		return new EmployeeSearchRequest(null, null, null, null, page, size, sort);
	}

	@Test
	void defaultSortIsIdAscending() {
		Pageable pageable = service.buildPageable(request(0, 10, null));

		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
	}

	@Test
	void sortFieldAndDirectionAreParsedWithIdAsTieBreaker() {
		Sort sort = service.parseSort("salary,DESC");

		assertThat(sort.getOrderFor("salary").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void unknownSortFieldIsRejected() {
		assertThatThrownBy(() -> service.parseSort("password,asc"))
				.isInstanceOf(InvalidPageRequestException.class)
				.hasMessageContaining("Invalid sort field");
	}

	@Test
	void unknownDirectionIsRejected() {
		assertThatThrownBy(() -> service.parseSort("name,sideways")).isInstanceOf(InvalidPageRequestException.class);
		assertThatThrownBy(() -> service.parseSort("name,asc,extra")).isInstanceOf(InvalidPageRequestException.class);
	}

	@Test
	void pageSizeMustBeBetweenOneAndMaximum() {
		assertThat(service.buildPageable(request(0, 100, null)).getPageSize()).isEqualTo(100);
		assertThatThrownBy(() -> service.buildPageable(request(0, 101, null)))
				.isInstanceOf(InvalidPageRequestException.class);
		assertThatThrownBy(() -> service.buildPageable(request(0, 0, null)))
				.isInstanceOf(InvalidPageRequestException.class);
		assertThatThrownBy(() -> service.buildPageable(request(-1, 10, null)))
				.isInstanceOf(InvalidPageRequestException.class);
	}

	@Test
	void minSalaryGreaterThanMaxIsRejectedBeforeQuerying() {
		EmployeeSearchRequest invalid = new EmployeeSearchRequest(null, null, new BigDecimal("90000"),
				new BigDecimal("30000"), 0, 10, null);

		assertThatThrownBy(() -> service.searchEmployees(invalid)).isInstanceOf(InvalidSalaryRangeException.class);
		verify(repository, never()).search(any(), any(), any(), any(), any());
	}

	@Test
	void filtersAreNormalizedBeforeQuerying() {
		when(repository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

		service.searchEmployees(new EmployeeSearchRequest("  IT ", "QaSim", null, null, 0, 10, null));

		// Department lower-cased and trimmed, name turned into a lower-case "contains" pattern.
		verify(repository).search(eq("it"), isNull(), isNull(), eq("%qasim%"), any(Pageable.class));
	}

	@Test
	void blankFiltersAreIgnored() {
		when(repository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

		service.searchEmployees(new EmployeeSearchRequest(" ", "", null, null, 0, 10, null));

		verify(repository).search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
	}
}
