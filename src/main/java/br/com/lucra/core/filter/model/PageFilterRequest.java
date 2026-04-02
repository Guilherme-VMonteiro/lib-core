package br.com.lucra.core.filter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.util.Objects.isNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageFilterRequest<T> {
	private String query;
	private List<FilterRule<T, ?>> filterRuleList;
	private List<SortRule> sortRuleList;
	private Integer page = 0;
	private Integer size = 10;
	
	public void setSize(Integer size) {
		validatePageSize(size);
		this.size = size;
	}
	
	private void validatePageSize(Integer size) {
		if (isNull(size) || !(size == 10 || size == 25 || size == 50)) {
			throw new IllegalArgumentException("Invalid page size: " + size);
		}
	}
}
