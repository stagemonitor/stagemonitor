package org.stagemonitor.configuration.source;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stagemonitor.core.configuration.RemotePropertiesConfigurationSource;
import org.stagemonitor.core.util.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class ConfigServerConfigurationSourceTest {
	@Test
	public void whenReturnCodeNot200_thenEmptyConfiguration() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "", 400, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertNotNull(dut);
	}

	@Test
	public void whenResponseIsEmpty_thenEmptyConfiguration() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, null, 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");
		Assert.assertNotNull(dut);

		prepMockWithResponse(http, "", 200, null);
		dut.reload();
		Assert.assertNotNull(dut);
	}

	@Test(expected = IllegalArgumentException.class)
	public void whenBadConfigServerAddress_thenThrowException() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, null, 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "malformedAddress");
	}

	@Test(expected = IllegalArgumentException.class)
	public void whenMissingConfigUrl_thenThrowException() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, null, 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "");
	}

	@Test
	public void whenMalformedResponse_thenEmptyConfiguration() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "malformed content", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertNotNull(dut);
	}

	@Test
	public void whenMalformedContent_givenAlsoValidProperty_thenReturnConfig() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "malformed content\nnew: hope\nbut not much", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertNotNull(dut);
		Assert.assertEquals("hope", dut.getValue("new"));
	}

	@Test
	public void whenIOException_thenEmptyConfiguration() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar", 200, new IOException("bad connection or connection loss"));
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");
		Assert.assertNotNull(dut);

		// Now the connection is back
		prepMockWithResponse(http, "foo: bar", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));

		// If connection is lost again, it should cache the value
		prepMockWithResponse(http, "", 200, new IOException("bad connection or connection loss"));
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));

		// Or the server isn't responding properly
		prepMockWithResponse(http, "", 503, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
	}

	@Test
	public void whenSimpleConfig_thenReturnConfig() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar\nuser.name: alice", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("alice", dut.getValue("user.name"));
	}

	@Test
	public void whenSimpleConfig_givenSpecialCharsInPropValue_thenReturnConfig() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar\nuser.name: some more complex 123 \" string", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("some more complex 123 \" string", dut.getValue("user.name"));

		// A quoted value is still a quoted value
		prepMockWithResponse(http, "foo: bar\nuser.name: \"alice\"\nfeature.abc: false", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("\"alice\"", dut.getValue("user.name"));
		Assert.assertEquals("false", dut.getValue("feature.abc"));
	}

	@Test
	public void whenSimpleConfig_givenDifferentUpdates_thenReturnConfig() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar\nuser.name: alice", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("alice", dut.getValue("user.name"));

		// An update adds a third value
		prepMockWithResponse(http, "foo: bar\nuser.name: alice\nfeature.abc: false", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("alice", dut.getValue("user.name"));
		Assert.assertEquals("false", dut.getValue("feature.abc"));

		// An update changes the second property
		prepMockWithResponse(http, "foo: bar\nuser.name: bob\nfeature.abc: false", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("bob", dut.getValue("user.name"));
		Assert.assertEquals("false", dut.getValue("feature.abc"));

		// An update deletes the second property
		prepMockWithResponse(http, "foo: bar\nfeature.abc: false", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals(null, dut.getValue("user.name"));
		Assert.assertEquals("false", dut.getValue("feature.abc"));

		// An update adds a line break
		prepMockWithResponse(http, "foo: bar\n\nfeature.abc: false", 200, null);
		dut.reload();
		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("false", dut.getValue("feature.abc"));
	}

	@Test
	public void whenSimpleConfig_givenPropertyList_thenReturnConfig() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar\nuser[0]: alice\nuser[1]: bob", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("alice", dut.getValue("user[0]"));
		Assert.assertEquals("bob", dut.getValue("user[1]"));
	}

	@Test
	public void whenPropertyContainsAColon_thenStillSplitKeyValueCorrectly() {
		HttpClient http = Mockito.mock(HttpClient.class);

		prepMockWithResponse(http, "foo: bar\nuser.name: alice:bob\nanother: one: bites\nthe: dust:\n", 200, null);
		final RemotePropertiesConfigurationSource dut = new RemotePropertiesConfigurationSource(http, "http://localhost/config");

		Assert.assertEquals("bar", dut.getValue("foo"));
		Assert.assertEquals("alice:bob", dut.getValue("user.name"));
		Assert.assertEquals("one: bites", dut.getValue("another"));
		Assert.assertEquals("dust:", dut.getValue("the"));
	}

	private void prepMockWithResponse(HttpClient mockedHttpClient, String response, Integer statusCode, IOException ioException) {
		final ByteArrayInputStream inputStream;
		try {
			inputStream = response == null ? null : new ByteArrayInputStream(response.getBytes("UTF-8"));

			Mockito.doAnswer(new Answer<Map<String, String>>() {
				@Override
				public Map<String, String> answer(InvocationOnMock invocation) throws Throwable {
					return (Map<String, String>) ((HttpClient.ResponseHandler) invocation.getArgument(4)).handleResponse(null, inputStream, statusCode, ioException);
				}
			}).when(mockedHttpClient)
					.send(
							ArgumentMatchers.anyString(),
							ArgumentMatchers.anyString(),
							ArgumentMatchers.any(Map.class),
							ArgumentMatchers.<HttpClient.OutputStreamHandler>any(),
							ArgumentMatchers.any(HttpClient.ResponseHandler.class));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
