package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Clock;
import org.junit.Test;

public class QuantizedClockTest {

	private final Clock delegate = mock(Clock.class);

	@Test
	public void getTime1() throws Exception {
		when(delegate.getTime()).thenReturn(1001L);
		assertEquals(1000, new QuantizedClock(delegate, 100).getTime());
	}

	@Test
	public void getTime2() throws Exception {
		when(delegate.getTime()).thenReturn(1999L);
		assertEquals(1900, new QuantizedClock(delegate, 100).getTime());
	}

	@Test
	public void getTime3() throws Exception {
		when(delegate.getTime()).thenReturn(1000L);
		assertEquals(1000, new QuantizedClock(delegate, 100).getTime());
	}
}