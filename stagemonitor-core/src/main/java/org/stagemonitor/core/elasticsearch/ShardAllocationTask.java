package org.stagemonitor.core.elasticsearch;

import java.util.Collections;
import java.util.TimerTask;

/**
 * A task that runs periodically and changes the required box_type of an index.
 * <p>
 * This means that all shards of an index are only allocated on nodes that have a specific box_type.
 * For instance, you could tag the node with node.box_type: hot in elasticsearch.yml,
 * or you could start a node using ./bin/elasticsearch --node.box_type hot
 * <p>
 * This implements the hot-cold architecture described in https://www.elastic.co/blog/hot-warm-architecture
 * where new data that is more frequently queried and updated is stored on beefy nodes (SSDs, more RAM and CPU).
 * When the indexes reach a certain age, they are allocated on cold nodes.
 */
public class ShardAllocationTask extends TimerTask {

	private final String indexPrefix;
	private final int moveToColdNodesAfterDays;
	private final ElasticsearchClient elasticsearchClient;
	private final String boxType;
	private final IndexSelector indexSelector;

	public ShardAllocationTask(IndexSelector indexSelector, String indexPrefix, int invokeForIndicesOlderThanDays,
							   ElasticsearchClient elasticsearchClient, String boxType) {
		this.indexSelector = indexSelector;
		this.indexPrefix = indexPrefix;
		this.moveToColdNodesAfterDays = invokeForIndicesOlderThanDays;
		this.elasticsearchClient = elasticsearchClient;
		this.boxType = boxType;
	}

	@Override
	public void run() {
		final String indexPatternOlderThanDays = indexSelector.getIndexPatternOlderThanDays(indexPrefix, moveToColdNodesAfterDays);
		elasticsearchClient.updateIndexSettings(indexPatternOlderThanDays, Collections.singletonMap("index.routing.allocation.require.box_type", boxType));
	}
}
