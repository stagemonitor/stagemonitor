package org.stagemonitor.core.elasticsearch;

import java.util.TimerTask;

/**
 * A task that runs periodically and calls _optimize on old Elasticsearch logstash-style indices.
 * <p/>
 * By optimizing, we are reducing the index size and make the overall index performance a bit faster.
 * But an optimisation process is I/O heavy.
 */
public class OptimizeIndicesTask extends TimerTask {

	private final String indexPrefix;
	private final int optimizeIndicesOlderThanDays;
	private final ElasticsearchClient elasticsearchClient;
	private final IndexSelector indexSelector;

	public OptimizeIndicesTask(IndexSelector indexSelector, String indexPrefix, int optimizeIndicesOlderThanDays, ElasticsearchClient elasticsearchClient) {
		this.indexSelector = indexSelector;
		this.indexPrefix = indexPrefix;
		this.optimizeIndicesOlderThanDays = optimizeIndicesOlderThanDays;
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public void run() {
		final String indexPatternOlderThanDays = indexSelector.getIndexPatternOlderThanDays(indexPrefix, optimizeIndicesOlderThanDays);
		elasticsearchClient.optimizeIndices(indexPatternOlderThanDays);
	}
}
