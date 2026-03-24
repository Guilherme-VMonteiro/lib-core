package br.com.lucra.core.filter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageFilterRequest<T> {
	private String query;
	private List<FilterRule<T, ?>> filterRuleList;
	private List<SortRule> sortRuleList;
	private Integer page = 0;
	private PageSize size = PageSize.TEN;
}
