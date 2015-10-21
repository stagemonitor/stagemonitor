package org.stagemonitor.core.elasticsearch;

import java.util.TimerTask;

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
		elasticsearchClient.optimizeIndices(indexSelector.getIndexPatternOlderThanDays(indexPrefix, optimizeIndicesOlderThanDays));
	}
}
