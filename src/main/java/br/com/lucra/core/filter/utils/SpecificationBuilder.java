package br.com.lucra.core.filter.utils;

import br.com.lucra.core.filter.model.FilterRule;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Array;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

@NoArgsConstructor(access = PRIVATE)
public final class SpecificationBuilder {
	
	private static final String FIELD_NULL_OR_BLANK_MESSAGE = "Filter field cannot be null or blank";
	private static final String INVALID_FILTER_FIELD = "Invalid filter field";
	private static final String INVALID_FILTER_FIELD_PATH = "Invalid filter field path";
	private static final String ACCENTED_CHARS =
			"\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF"
			+ "\u00D2\u00D3\u00D4\u00D5\u00D6\u00D9\u00DA\u00DB\u00DC\u00C7\u00D1"
			+ "\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF"
			+ "\u00F2\u00F3\u00F4\u00F5\u00F6\u00F9\u00FA\u00FB\u00FC\u00E7\u00F1";
	private static final String UNACCENTED_CHARS =
			"AAAAAAEEEEIIIIOOOOOUUUUCN"
			+ "aaaaaaeeeeiiiiooooouuuucn";
	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
	
	public static <T> Specification<T> build(List<FilterRule<T, ?>> filters) {
		return build(filters, null, null);
	}
	
	public static <T> Specification<T> build(List<FilterRule<T, ?>> filters, String query, List<String> queryFields) {
		return (root, queryJpa, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			
			// Add filter rule predicates
			if (nonNull(filters) && !filters.isEmpty()) {
				for (FilterRule<T, ?> filter : filters) {
					Predicate predicate = toPredicate(filter, root, criteriaBuilder);
					if (nonNull(predicate)) predicates.add(predicate);
				}
			}
			
			// Add query predicates
			if (nonNull(query) && !isBlank(query) && nonNull(queryFields) && !queryFields.isEmpty()) {
				List<Predicate> queryPredicates = new ArrayList<>();
				for (String field : queryFields) {
					try {
						Path<String> path = resolvePath(root, field);
						if (String.class.equals(path.getJavaType())) {
							Expression<String> normalizedPath = normalizeTextExpression(path.as(String.class), criteriaBuilder);
							String pattern = "%" + escapeLike(normalizeTextValue(query)) + "%";
							queryPredicates.add(criteriaBuilder.like(normalizedPath, pattern, '\\'));
						}
					} catch (IllegalArgumentException ignored) {
						// Skip invalid fields
					}
				}
				
				if (!queryPredicates.isEmpty()) {
					predicates.add(criteriaBuilder.or(queryPredicates.toArray(new Predicate[0])));
				}
			}
			
			if (predicates.isEmpty()) return criteriaBuilder.conjunction();
			
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
	
	private static <T, V> Predicate toPredicate(FilterRule<T, V> filter, Root<T> root, CriteriaBuilder criteriaBuilder) {
		if (isNull(filter) || isNull(filter.getField()) || isNull(filter.getFilterOperation())) return null;
		
		Path<V> path = resolvePath(root, filter.getField());
		Object rawValue = filter.getFilterValue();
		boolean isNullValue = isNull(rawValue);
		
		return switch (filter.getFilterOperation()) {
			case EQUAL -> equal(path, rawValue, criteriaBuilder, isNullValue);
			case NOT_EQUAL -> notEqual(path, rawValue, criteriaBuilder, isNullValue);
			case GREATER_THAN -> compare(path, rawValue, criteriaBuilder, ComparisonType.GREATER_THAN);
			case LESS_THAN -> compare(path, rawValue, criteriaBuilder, ComparisonType.LESS_THAN);
			case GREATER_OR_EQUAL -> compare(path, rawValue, criteriaBuilder, ComparisonType.GREATER_OR_EQUAL);
			case LESS_OR_EQUAL -> compare(path, rawValue, criteriaBuilder, ComparisonType.LESS_OR_EQUAL);
			case CONTAINED -> contained(path, rawValue, criteriaBuilder);
			case NOT_CONTAINED -> criteriaBuilder.not(contained(path, rawValue, criteriaBuilder));
		};
	}
	
	@SuppressWarnings("unchecked")
	private static <T, V> Path<V> resolvePath(Root<T> root, String fieldPath) {
		if (isNull(fieldPath) || isBlank(fieldPath)) throw new IllegalArgumentException(FIELD_NULL_OR_BLANK_MESSAGE);
		
		String[] tokens = fieldPath.split("\\.");
		Path<?> currentPath = root;
		
		try {
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim();
				if (token.isEmpty())
					throw new IllegalArgumentException(String.format("%s: '%s'", INVALID_FILTER_FIELD_PATH, fieldPath));
				
				if (i < tokens.length - 1 && currentPath instanceof From<?, ?> fromPath) {
					currentPath = fromPath.join(token);
				} else {
					currentPath = currentPath.get(token);
				}
			}
			
			return (Path<V>) currentPath;
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(String.format("%s: '%s'", INVALID_FILTER_FIELD, fieldPath), ex);
		}
	}
	
	private static Predicate compare(Path<?> path, Object value, CriteriaBuilder criteriaBuilder, ComparisonType comparisonType) {
		if (isNull(value)) throw new IllegalArgumentException("Filter value cannot be null for comparison operations");
		
		Class<?> javaType = path.getJavaType();
		if (!Comparable.class.isAssignableFrom(javaType))
			throw new IllegalArgumentException("Field type must implement Comparable for comparison operations");
		
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
	
	private static Predicate equal(Path<?> path, Object rawValue, CriteriaBuilder criteriaBuilder, boolean isNullValue) {
		if (isNullValue) return criteriaBuilder.isNull(path);
		
		if (isTextComparison(path, rawValue)) {
			Expression<String> normalizedPath = normalizeTextExpression(path.as(String.class), criteriaBuilder);
			String normalizedValue = normalizeTextValue((String) rawValue);
			return criteriaBuilder.equal(normalizedPath, normalizedValue);
		}
		
		return criteriaBuilder.equal(path, rawValue);
	}
	
	private static Predicate notEqual(Path<?> path, Object rawValue, CriteriaBuilder criteriaBuilder, boolean isNullValue) {
		if (isNullValue) return criteriaBuilder.isNotNull(path);
		
		if (isTextComparison(path, rawValue)) {
			Expression<String> normalizedPath = normalizeTextExpression(path.as(String.class), criteriaBuilder);
			String normalizedValue = normalizeTextValue((String) rawValue);
			return criteriaBuilder.notEqual(normalizedPath, normalizedValue);
		}
		
		return criteriaBuilder.notEqual(path, rawValue);
	}
	
	private static boolean isTextComparison(Path<?> path, Object rawValue) {
		return String.class.equals(path.getJavaType()) && rawValue instanceof String;
	}
	
	private static Expression<String> normalizeTextExpression(Expression<String> expression, CriteriaBuilder criteriaBuilder) {
		Expression<String> translated = criteriaBuilder.function(
				"translate",
				String.class,
				expression,
				criteriaBuilder.literal(ACCENTED_CHARS),
				criteriaBuilder.literal(UNACCENTED_CHARS)
		);
		return criteriaBuilder.lower(translated);
	}
	
	private static String normalizeTextValue(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
		String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
		return withoutDiacritics.toLowerCase(Locale.ROOT);
	}
	
	private static Predicate contained(Path<?> path, Object rawValue, CriteriaBuilder criteriaBuilder) {
		if (isNull(rawValue)) return criteriaBuilder.disjunction();
		
		boolean isTextPath = String.class.equals(path.getJavaType());
		if (isTextPath && rawValue instanceof String textValue) {
			Expression<String> normalizedPath = normalizeTextExpression(path.as(String.class), criteriaBuilder);
			String pattern = "%" + escapeLike(normalizeTextValue(textValue)) + "%";
			return criteriaBuilder.like(normalizedPath, pattern, '\\');
		}
		
		Expression<?> targetExpression = isTextPath ? normalizeTextExpression(path.as(String.class), criteriaBuilder) : path;
		CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(targetExpression);
		
		if (rawValue instanceof Collection<?> collection) {
			for (Object value : collection) {
				inClause.value(normalizeInValue(value, isTextPath));
			}
			
			return inClause;
		}
		
		if (rawValue.getClass().isArray()) {
			int length = Array.getLength(rawValue);
			
			for (int i = 0; i < length; i++) {
				inClause.value(normalizeInValue(Array.get(rawValue, i), isTextPath));
			}
			
			return inClause;
		}
		
		inClause.value(normalizeInValue(rawValue, isTextPath));
		return inClause;
	}
	
	private static Object normalizeInValue(Object value, boolean isTextPath) {
		return (isTextPath && value instanceof String textValue) ? normalizeTextValue(textValue) : value;
	}
	
	private static String escapeLike(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
	
	private enum ComparisonType {
		GREATER_THAN,
		LESS_THAN,
		GREATER_OR_EQUAL,
		LESS_OR_EQUAL
	}
}
