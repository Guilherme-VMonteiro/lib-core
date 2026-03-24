package br.com.lucra.core.filter.model;

import jakarta.persistence.metamodel.SingularAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterRule<T, V> {
	private SingularAttribute<? super T, V> field;
	private FilterOperation filterOperation;
	private Object filterValue;
}
