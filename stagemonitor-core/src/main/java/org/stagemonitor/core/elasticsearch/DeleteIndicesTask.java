package org.stagemonitor.core.elasticsearch;

import java.util.TimerTask;

public class DeleteIndicesTask extends TimerTask {

	private final String indexPrefix;
	private final int deleteIndicesOlderThanDays;
	private final ElasticsearchClient elasticsearchClient;
	private final IndexSelector indexSelector;

	public DeleteIndicesTask(IndexSelector indexSelector, String indexPrefix, int deleteIndicesOlderThanDays, ElasticsearchClient elasticsearchClient) {
		this.indexSelector = indexSelector;
		this.indexPrefix = indexPrefix;
		this.deleteIndicesOlderThanDays = deleteIndicesOlderThanDays;
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public void run() {
		elasticsearchClient.deleteIndices(indexSelector.getIndexPatternOlderThanDays(indexPrefix, deleteIndicesOlderThanDays));
	}
}
