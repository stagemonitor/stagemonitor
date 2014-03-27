package org.stagemonitor.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GraphiteEncoderTest {

	@Test
	public void testGraphiteEncodingDecoding() throws Exception {
		final String original = "GET /asdf/{id}/bla.html?a=c;a=b&bla=blubb";
		final String encoded = GraphiteEncoder.encodeForGraphite(original);
		assertEquals("GET%20%2Fasdf%2F%7Bid%7D%2Fbla%2ehtml%3Fa%3Dc%3Ba%3Db%26bla%3Dblubb", encoded);
		assertEquals(original, GraphiteEncoder.decodeForGraphite(encoded));
	}

}
