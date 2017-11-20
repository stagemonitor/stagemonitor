package org.stagemonitor.configuration.converter;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UrlValueConverterTest {

	private final ValueConverter<URL> converter = UrlValueConverter.INSTANCE;

	@Test
	public void testConvert() throws Exception {
		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(converter.toString(converter.convert("http://www.example.com"))).isEqualTo("http://www.example.com");
			softly.assertThat(converter.toString(converter.convert("http://www.example.com/"))).isEqualTo("http://www.example.com");
			softly.assertThat(converter.toString(converter.convert("http://www.example.com/"))).isEqualTo("http://www.example.com");
			softly.assertThat(converter.convert("http://user:pwd@www.example.com").getUserInfo()).isEqualTo("user:pwd");
		});
	}

	@Test
	public void testToSafeString() throws Exception {
		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(converter.toSafeString(converter.convert("http://user:pwd@www.example.com"))).isEqualTo("http://user:XXX@www.example.com");
			softly.assertThat(converter.toSafeString(converter.convert("https://user:pw:d@www.example.com"))).isEqualTo("https://user:XXX@www.example.com");
		});
	}

	@Test
	public void testToSafeStringList() throws Exception {
		final ListValueConverter<URL> urlListValueConverter = new ListValueConverter<>(converter);
		assertThat(urlListValueConverter.toSafeString(urlListValueConverter.convert("http://user:pwd@www.example.com,https://user:pw:d@www.example.com")))
				.isEqualTo("http://user:XXX@www.example.com,https://user:XXX@www.example.com");
	}

	@Test
	public void testConvertNull() throws Exception {
		assertThatThrownBy(() -> converter.convert(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testConvertInvalid() throws Exception {
		assertThatThrownBy(() -> converter.convert("invalid"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testConvertNoProtocol() throws Exception {
		assertThatThrownBy(() -> converter.convert("www.example.com"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("no protocol");
	}

	@Test
	public void testToStringNull() throws Exception {
		assertThat(converter.toString(null)).isNull();
	}

}
