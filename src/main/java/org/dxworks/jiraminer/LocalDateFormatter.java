package org.dxworks.jiraminer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateFormatter {
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static LocalDate parse(String dateString) {
		return LocalDate.parse(dateString, dateTimeFormatter);
	}

	public static String format(LocalDate date) {
		return date.format(dateTimeFormatter);
	}
}
