package org.stagemonitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.util.JsonUtils;

public class ConfigurationSourceExporter {

	public static void main(String[] args) throws JsonProcessingException {
		StageMonitor.startMonitoring(new MeasurementSession("test", "test", "test"));
		System.out.println(JsonUtils.toJson(StageMonitor.getConfiguration().getConfigurationOptionsByPlugin()));
	}
}
