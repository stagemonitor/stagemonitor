package org.stagemonitor.jdbc.p6spy;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;

import java.util.LinkedList;
import java.util.List;

public class P6SpyMultiLogger implements P6Logger {

	private static List<P6Logger> loggers = new LinkedList<P6Logger>();

	public static void addLogger(P6Logger logger) {
		if (logger != null) {
			loggers.add(logger);
		}
	}

	@Override
	public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		for (P6Logger logger : loggers) {
			if (logger.isCategoryEnabled(category)) {
				logger.logSQL(connectionId, now, elapsed, category, prepared, sql);
			}
		}
	}

	@Override
	public void logException(Exception e) {
		for (P6Logger logger : loggers) {
			logger.logException(e);
		}
	}

	@Override
	public void logText(String text) {
		for (P6Logger logger : loggers) {
			logger.logText(text);
		}
	}

	@Override
	public boolean isCategoryEnabled(Category category) {
		boolean result = false;
		for (P6Logger logger : loggers) {
			result |= logger.isCategoryEnabled(category);
		}
		return result;
	}
}
