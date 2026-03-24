package br.com.lucra.core.filter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PageSize {
	TEN(10),
	TWENTY_FIVE(25),
	FIFTY(50);
	
	private final int value;
	
	public static PageSize fromValue(int value) {
		for (PageSize ps : values()) {
			if (ps.value == value) return ps;
		}
		
		throw new IllegalArgumentException("Invalid page size: " + value);
	}
}
