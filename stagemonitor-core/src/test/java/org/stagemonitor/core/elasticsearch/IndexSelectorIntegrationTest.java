package org.stagemonitor.core.elasticsearch;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.Clock;
import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class IndexSelectorIntegrationTest extends AbstractElasticsearchTest {

	private IndexSelector indexSelector;

	@Test
	public void testDeleteOldIndices() throws Exception {
		setTime(0L);
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1969.12.30/metrics", "{\"foo\":\"bar\"}");
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1969.12.31/metrics", "{\"foo\":\"bar\"}");
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1970.01.01/metrics", "{\"foo\":\"bar\"}");
		refresh();
		assertEquals(getIndices("stagemonitor-metrics*"),
				asSet("stagemonitor-metrics-1969.12.30", "stagemonitor-metrics-1969.12.31", "stagemonitor-metrics-1970.01.01"));

		elasticsearchClient.deleteIndices(indexSelector.getIndexPatternOlderThanDays("stagemonitor-metrics-", 1));
		refresh();

		assertEquals(getIndices("stagemonitor-metrics*"),
				asSet("stagemonitor-metrics-1969.12.31", "stagemonitor-metrics-1970.01.01"));
	}

	@Test
	public void testDeleteOldIndicesUnavailable() throws Exception {
		setTime(new GregorianCalendar(1970, 0, 10, 1, 0, 0).getTimeInMillis());
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1970.01.07/metrics", "{\"foo\":\"bar\"}");
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1970.01.09/metrics", "{\"foo\":\"bar\"}");
		elasticsearchClient.sendAsJson("POST", "/stagemonitor-metrics-1970.01.10/metrics", "{\"foo\":\"bar\"}");
		refresh();
		assertEquals(asSet("stagemonitor-metrics-1970.01.07", "stagemonitor-metrics-1970.01.09", "stagemonitor-metrics-1970.01.10"),
				getIndices("stagemonitor-metrics*"));

		elasticsearchClient.deleteIndices(indexSelector.getIndexPatternOlderThanDays("stagemonitor-metrics-", 1));
		refresh();

		assertEquals(asSet("stagemonitor-metrics-1970.01.09", "stagemonitor-metrics-1970.01.10"),
				getIndices("stagemonitor-metrics*"));
	}

	private void setTime(long time) {
		Clock clock = Mockito.mock(Clock.class);
		when(clock.getTime()).thenReturn(time);
		indexSelector = new IndexSelector(clock);
	}


	private Set<String> asSet(String... strings) {
		return new HashSet<String>(asList(strings));
	}

	private Set<String> getIndices(String indexPattern) {
		return new HashSet<String>(asList(client.admin().indices().prepareGetIndex().addIndices(indexPattern).get().indices()));
	}
}
