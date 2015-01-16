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
	private int sensitivity;
	private List<Check> checks;

	public List<Check.Result> checkAll(Map<String, Double> actualValuesByMetric) {
		List<Check.Result> results = new ArrayList<Check.Result>(checks.size());
		for (Check check : checks) {
			Check.Result result = check.check(target.toString(), actualValuesByMetric.get(check.getMetric()));
			results.add(result);
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

	public int getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(int sensitivity) {
		this.sensitivity = sensitivity;
	}
}
