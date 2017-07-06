package org.stagemonitor.web.servlet.eum;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientSpanBooleanTagProcessorTest {

	@Test
	public void parseBooleanOrFalse_returnsTrueFor1() throws Exception {
		assertThat(ClientSpanBooleanTagProcessor.parseBooleanOrFalse("1")).isTrue();
	}

	@Test
	public void parseBooleanOrFalse_returnsTrueForTrue() throws Exception {
		assertThat(ClientSpanBooleanTagProcessor.parseBooleanOrFalse("true")).isTrue();
	}

	@Test
	public void testParseBooleanOrFalse_returnsFalseFor0() throws Exception {
		assertThat(ClientSpanBooleanTagProcessor.parseBooleanOrFalse("0")).isFalse();
	}

	@Test
	public void testParseBooleanOrFalse_returnsFalseForNull() throws Exception {
		assertThat(ClientSpanBooleanTagProcessor.parseBooleanOrFalse(null)).isFalse();
	}

	@Test
	public void parseBooleanOrFalse_returnsFalseForFalse() throws Exception {
		assertThat(ClientSpanBooleanTagProcessor.parseBooleanOrFalse("false")).isFalse();
	}
}
