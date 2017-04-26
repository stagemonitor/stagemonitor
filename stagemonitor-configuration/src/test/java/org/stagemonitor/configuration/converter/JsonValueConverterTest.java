package org.stagemonitor.configuration.converter;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;

public class JsonValueConverterTest {

	private JsonValueConverter<Map<String, TestObject>> jsonValueConverter =
			new JsonValueConverter<Map<String, TestObject>>(new TypeReference<Map<String, TestObject>>(){});

	@Test
	public void testConvert() throws Exception {
		Map<String, TestObject> convert = jsonValueConverter.convert("{ \"1\": { \"test\": \"foobar\" } }");
		Assert.assertEquals("foobar", convert.get("1").getTest());
	}

	public static class TestObject {
		private String test;
		private String getTest() {
			return test;
		}
		private void setTest(String test) {
			this.test = test;
		}
	}
}
