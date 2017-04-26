package org.stagemonitor.core.elasticsearch;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.codahale.metrics.Clock;
import org.stagemonitor.util.StringUtils;

public class IndexSelector {

	private final Clock clock;

	public IndexSelector(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Returns an Elasticsearch index pattern that only includes indices that are older than the specified amount of days
	 * <p/>
	 * Your indices are ought to have the pattern [prefix]YYYY.MM.DD
	 * <p/>
	 * See also https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-index.html
	 * @param prefix the index prefix e.g. 'stagemonitor-metrics-'
	 * @param days the number days that should be excluded
	 * @return The Elasticsearch index pattern that only includes indices that are older than the specified amount of days
	 */
	public String getIndexPatternOlderThanDays(String prefix, int days) {
		// select all indices, then exclude months and days that are not older than the days parameter
		// Example:
		// stagemonitor-metrics-*,-stagemonitor-metrics-2015.10.*,-stagemonitor-metrics-2015.09.30
		final StringBuilder indexPattern = new StringBuilder(prefix).append('*');

		final GregorianCalendar now = getNowUTC();
		final GregorianCalendar lastDayToExclude = getLastDayToExclude(days);
		final GregorianCalendar alreadyExcluded = getNowUTC();

		excludeMonths(prefix, now, indexPattern, lastDayToExclude, alreadyExcluded);
		excludeDays(prefix, indexPattern, lastDayToExclude, alreadyExcluded);

		return indexPattern.toString();
	}

	private void excludeMonths(String prefix, GregorianCalendar now, StringBuilder sb, GregorianCalendar lastDayToExclude, GregorianCalendar alreadyExcluded) {
		int excludedMonths = now.get(Calendar.MONTH) - lastDayToExclude.get(Calendar.MONTH);
		if (excludedMonths < 0) {
			excludedMonths += 12;
		}

		for (int i = 0; i < excludedMonths; i++) {
			// ,[prefix]YYYY.MM.*
			final int year = alreadyExcluded.get(Calendar.YEAR);
			final int month = alreadyExcluded.get(Calendar.MONTH);
			sb.append(",-")
					.append(prefix)
					.append(formatTwoDigitsLeadingZero(year)).append('.')
					.append(formatTwoDigitsLeadingZero(month + 1)).append(".*");

			alreadyExcluded.set(year, month, 0);
		}
	}

	private void excludeDays(String prefix, StringBuilder sb, GregorianCalendar lastDayToExclude, GregorianCalendar alreadyExcluded) {
		for (; alreadyExcluded.after(lastDayToExclude) || alreadyExcluded.equals(lastDayToExclude); alreadyExcluded.add(Calendar.DAY_OF_YEAR, -1)) {
			sb.append(",-").append(prefix).append(StringUtils.getLogstashStyleDate(alreadyExcluded.getTimeInMillis()));
		}
	}

	private GregorianCalendar getLastDayToExclude(int days) {
		final GregorianCalendar lastDayToExclude = getNowUTC();
		lastDayToExclude.add(Calendar.DAY_OF_YEAR, days * -1);
		return lastDayToExclude;
	}

	private GregorianCalendar getNowUTC() {
		final GregorianCalendar now = new GregorianCalendar();
		now.setTimeZone(TimeZone.getTimeZone("UTC"));
		now.setTime(new Date(clock.getTime()));
		return now;
	}

	private static String formatTwoDigitsLeadingZero(int i) {
		return String.format("%02d", i);
	}
}
