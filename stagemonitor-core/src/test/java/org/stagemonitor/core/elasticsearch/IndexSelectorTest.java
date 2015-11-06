package org.stagemonitor.core.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.core.util.DateUtils;

public class IndexSelectorTest {

	private Clock clock;
	private IndexSelector indexSelector;

	@Before
	public void setUp() throws Exception {
		clock = Mockito.mock(Clock.class);
		when(clock.getTime()).thenReturn(0L);
		indexSelector = new IndexSelector(clock);
	}

	@Test
	public void testSelectIndexYearChange2Days() throws Exception {
		final String indexPattern = indexSelector.getIndexPatternOlderThanDays("metrics-", 2);
		assertEquals("metrics-*,-metrics-1970.01.*,-metrics-1969.12.31,-metrics-1969.12.30", indexPattern);
	}

	@Test
	public void testSelectIndexYearChange33Days() throws Exception {
		final String indexPattern = indexSelector.getIndexPatternOlderThanDays("metrics-", 33);
		assertEquals("metrics-*,-metrics-1970.01.*,-metrics-1969.12.*,-metrics-1969.11.30,-metrics-1969.11.29", indexPattern);
	}

	@Test
	public void testSelectIndexIntraMonth() throws Exception {
		when(clock.getTime()).thenReturn(DateUtils.getDayInMillis() * 7);
		final String indexPattern = indexSelector.getIndexPatternOlderThanDays("metrics-", 2);
		assertEquals("metrics-*,-metrics-1970.01.08,-metrics-1970.01.07,-metrics-1970.01.06", indexPattern);
	}
}