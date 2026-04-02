package br.com.lucra.core.filter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterRule<T, V> {
	private String field;
	private FilterOperation filterOperation;
	private Object filterValue;
}
