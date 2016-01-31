package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;

import com.codahale.metrics.Meter;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestTrace;

/**
 * Allows implementers to customize or omit reporting a request trace to Elasticsearch
 */
public interface ElasticsearchRequestTraceReporterInterceptor {

	void init(Configuration configuration);

	/**
	 * This method is called before a request trace gets reported to Elasticsearch.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the request trace or to exclude certain properties.
	 *
	 * @param requestTrace       The request trace that is about to be reported
	 * @param reportingRate      The rate at which request traces got reported
	 * @param excludedProperties Add the names of properties you want to exclude from a report
	 * @return <code>true</code> if the request trace should be reported, <code>false</code> if reporting should be omitted
	 */
	boolean interceptReport(RequestTrace requestTrace, Meter reportingRate, Collection<String> excludedProperties);
}
