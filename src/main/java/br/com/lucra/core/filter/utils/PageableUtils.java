package br.com.lucra.core.filter.utils;

import br.com.lucra.core.filter.model.PageFilterRequest;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class PageableUtils {
	
	public static Pageable toPageable(PageFilterRequest<?> pageFilterRequest) {
		return PageRequest.of( //
				pageFilterRequest.getPage(), //
				pageFilterRequest.getSize(), //
				Sort.by( //
						pageFilterRequest.getSortRuleList().stream() //
								.map(sortRule -> new Sort.Order( //
										sortRule.getDirection(), //
										sortRule.getField()) //
								) //
								.toList() //
				) //
		); //
	}
}
