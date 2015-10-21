package org.stagemonitor.core.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class DateUtils {

	private DateUtils() {
		// don't instantiate
	}

	public static Date getNextDateAtHour(int hour) {
		final GregorianCalendar now = new GregorianCalendar();

		final GregorianCalendar nextDate = new GregorianCalendar();
		// not truncating minutes to distribute load more evenly when multiple instances are started
		nextDate.set(Calendar.HOUR_OF_DAY, hour);
		if (nextDate.before(now)) {
			nextDate.add(Calendar.DAY_OF_YEAR, 1);
		}

		return nextDate.getTime();
	}

	public static long getDayInMillis() {
		return 1000 * 60 * 60 * 24;
	}
}
