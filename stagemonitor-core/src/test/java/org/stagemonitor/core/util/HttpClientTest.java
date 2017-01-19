package org.stagemonitor.core.util;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpClientTest {

	private HttpClient httpClient;

	@Before
	public void setup() {
		httpClient = new HttpClient();
	}

	@Test
	public void testNoNullPointerExceptionOnSend() throws IOException {
		try {
			httpClient.send("POST", "incorrect-url", null, null);
		} catch (NullPointerException t) {
			Assert.fail("Shouldn't throw NPE");
		} catch (Exception ignore) {
			//whatever
		}
	}
}
