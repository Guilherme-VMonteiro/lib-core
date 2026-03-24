package br.com.lucra.core.filter.utils;

import br.com.lucra.core.filter.model.PageFilterRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

	private PageableUtils() {
	}

	public static Pageable toPageable(PageFilterRequest<?> pageFilterRequest) {
		return PageRequest.of(
				pageFilterRequest.getPage(),
				pageFilterRequest.getSize().getValue(),
				Sort.by(
						pageFilterRequest.getSortRuleList().stream()
								.map(sortRule -> new Sort.Order(
										sortRule.getDirection(),
										sortRule.getField()
								))
								.toList()
				)
		);
	}
}
