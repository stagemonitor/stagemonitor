package org.stagemonitor.configuration.converter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClassInstanceValueConverterTest {

	interface Strategy {
	}

	public static class StrategyImpl implements Strategy {
	}

	static class PrivateStrategyImpl implements Strategy {
		private PrivateStrategyImpl() {
		}
	}

	@Test
	public void testSuccess() throws Exception {
		final ClassInstanceValueConverter<Strategy> converter = ClassInstanceValueConverter.of(Strategy.class);
		assertThat(converter.convert(StrategyImpl.class.getName())).isInstanceOf(StrategyImpl.class);
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		final ClassInstanceValueConverter<Strategy> converter = ClassInstanceValueConverter.of(Strategy.class);
		assertThatThrownBy(() -> converter.convert(PrivateStrategyImpl.class.getName()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Did not find a public no arg constructor for");
	}

	@Test
	public void testNotAnInstanceOfStrategy() throws Exception {
		final ClassInstanceValueConverter<Strategy> converter = ClassInstanceValueConverter.of(Strategy.class);
		assertThatThrownBy(() -> converter.convert(String.class.getName()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("is not an instance of");
	}
}
