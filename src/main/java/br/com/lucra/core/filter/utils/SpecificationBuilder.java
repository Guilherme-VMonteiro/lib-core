package br.com.lucra.core.filter.utils;

import br.com.lucra.core.filter.model.FilterRule;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.isNull;

public final class SpecificationBuilder {

	private SpecificationBuilder() {
	}

	public static <T> Specification<T> build(List<FilterRule<T, ?>> filters) {
		return (root, query, criteriaBuilder) -> {
			if (filters == null || filters.isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			List<Predicate> predicates = new ArrayList<>();
			for (FilterRule<T, ?> filter : filters) {
				Predicate predicate = toPredicate(filter, root, criteriaBuilder);
				if (predicate != null) {
					predicates.add(predicate);
				}
			}

			if (predicates.isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static <T, V> Predicate toPredicate(FilterRule<T, V> filter, Root<T> root, CriteriaBuilder criteriaBuilder) {
		if (filter == null || filter.getField() == null || filter.getFilterOperation() == null) {
			return null;
		}

		Path<V> path = root.get(filter.getField());
		Object rawValue = filter.getFilterValue();
		boolean isNullValue = isNull(rawValue);

		return switch (filter.getFilterOperation()) {
			case EQUAL -> isNullValue ? criteriaBuilder.isNull(path) : criteriaBuilder.equal(path, rawValue);
			case NOT_EQUAL -> isNullValue ? criteriaBuilder.isNotNull(path) : criteriaBuilder.notEqual(path, rawValue);
			case GREATER_THAN -> compare(path, rawValue, criteriaBuilder, ComparisonType.GREATER_THAN);
			case LESS_THAN -> compare(path, rawValue, criteriaBuilder, ComparisonType.LESS_THAN);
			case GREATER_OR_EQUAL -> compare(path, rawValue, criteriaBuilder, ComparisonType.GREATER_OR_EQUAL);
			case LESS_OR_EQUAL -> compare(path, rawValue, criteriaBuilder, ComparisonType.LESS_OR_EQUAL);
			case IN -> in(path, rawValue, criteriaBuilder);
			case NOT_IN -> criteriaBuilder.not(in(path, rawValue, criteriaBuilder));
		};
	}

	private static Predicate compare(Path<?> path, Object value, CriteriaBuilder criteriaBuilder, ComparisonType comparisonType) {
		if (isNull(value)) throw new IllegalArgumentException("Filter value cannot be null for comparison operations");

		Class<?> javaType = path.getJavaType();
		if (!Comparable.class.isAssignableFrom(javaType)) throw new IllegalArgumentException("Field type must implement Comparable for comparison operations");

		@SuppressWarnings("unchecked")
		Class<? extends Comparable<?>> comparableType = (Class<? extends Comparable<?>>) javaType;
		return compareTyped(path, value, criteriaBuilder, comparisonType, comparableType);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Predicate compareTyped(
			Path<?> path,
			Object value,
			CriteriaBuilder criteriaBuilder,
			ComparisonType comparisonType,
			Class<? extends Comparable<?>> valueType
	) {
		Expression expression = path.as((Class) valueType);
		Comparable typedValue = (Comparable) valueType.cast(value);

		return switch (comparisonType) {
			case GREATER_THAN -> criteriaBuilder.greaterThan(expression, typedValue);
			case LESS_THAN -> criteriaBuilder.lessThan(expression, typedValue);
			case GREATER_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(expression, typedValue);
			case LESS_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(expression, typedValue);
		};
	}

	private enum ComparisonType {
		GREATER_THAN,
		LESS_THAN,
		GREATER_OR_EQUAL,
		LESS_OR_EQUAL
	}

	private static Predicate in(Path<?> path, Object rawValue, CriteriaBuilder criteriaBuilder) {
		CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(path);

		if (isNull(rawValue)) return criteriaBuilder.disjunction();

		if (rawValue instanceof Collection<?> collection) {
			for (Object value : collection) {
				inClause.value(value);
			}
			
			return inClause;
		}

		if (rawValue.getClass().isArray()) {
			int length = Array.getLength(rawValue);
			
			for (int i = 0; i < length; i++) {
				inClause.value(Array.get(rawValue, i));
			}
			
			return inClause;
		}

		inClause.value(rawValue);
		return inClause;
	}
}
