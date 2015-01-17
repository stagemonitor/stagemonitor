package org.stagemonitor.alerting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A {@link CheckGroup} is a named collection of {@link Check}s with the same {@link MetricCategory} and target.
 * <p/>
 * Example: We have a check group named 'Search response time' with two checks both have the target
 * <code>requests.search.time</code>. The first check is that the 75th
 * percentile shall be < 4sec and the second one says the 99th percentile shall be < 10 sec.
 */
public class CheckGroup {

	private String id = UUID.randomUUID().toString();
	private String name;
	private MetricCategory metricCategory;
	private Pattern target;
	private int alertAfterXFailures = 1;
	private List<Check> checks;
	private String application;

	/**
	 * Performs threshold checks for the whole check group
	 *
	 * @param currentValuesByMetric the values of the target
	 * @param actualTarget the actual target that matched the {@link #target} pattern
	 * @return a list of check results (results with OK statuses are omitted)
	 */
	public List<Check.Result> checkAll(String actualTarget, Map<String, Double> currentValuesByMetric) {
		List<Check.Result> results = new ArrayList<Check.Result>(checks.size());
		for (Check check : checks) {
			Check.Result result = check.check(actualTarget, currentValuesByMetric.get(check.getMetric()));
			if (result.getStatus() != Check.Status.OK) {
				results.add(result);
			}
		}
		return Check.Result.getResultsWithMostSevereStatus(results);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pattern getTarget() {
		return target;
	}

	public void setTarget(Pattern target) {
		this.target = target;
	}

	public List<Check> getChecks() {
		return checks;
	}

	public void setChecks(List<Check> checks) {
		this.checks = checks;
	}

	public MetricCategory getMetricCategory() {
		return metricCategory;
	}

	public void setMetricCategory(MetricCategory metricCategory) {
		this.metricCategory = metricCategory;
	}

	public int getAlertAfterXFailures() {
		return alertAfterXFailures;
	}

	public void setAlertAfterXFailures(int alertAfterXFailures) {
		this.alertAfterXFailures = alertAfterXFailures;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}
}
