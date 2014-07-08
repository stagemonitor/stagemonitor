package org.stagemonitor.jdbc.p6spy;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.P6Logger;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.util.concurrent.TimeUnit;

public class StagemonitorP6Logger implements P6Logger {

	@Override
	public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql) {
		if (sql != null && !sql.isEmpty()) {
			// TODO config key to log prepared (without parameters)
			Profiler.addCall(sql, TimeUnit.MILLISECONDS.toNanos(elapsed));
		}
	}

	@Override
	public void logException(Exception e) {
	}

	@Override
	public void logText(String text) {
	}

	@Override
	public boolean isCategoryEnabled(Category category) {
		return Category.STATEMENT.equals(category);
	}
}