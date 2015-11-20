package org.stagemonitor.core.elasticsearch;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task that runs periodically and deletes old Elasticsearch logstash-style indices
 */
public class DeleteIndicesTask extends TimerTask {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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
		final String indexPatternOlderThanDays = indexSelector.getIndexPatternOlderThanDays(indexPrefix, deleteIndicesOlderThanDays);
		logger.info("Deleting indices: {}", indexPatternOlderThanDays);
		elasticsearchClient.deleteIndices(indexPatternOlderThanDays);
	}
}
