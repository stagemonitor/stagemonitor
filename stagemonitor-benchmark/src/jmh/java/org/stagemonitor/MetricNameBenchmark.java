package org.stagemonitor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.util.ArrayList;
import java.util.List;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

@State(value = Scope.Benchmark)
public class MetricNameBenchmark {

	private static final MetricName METRIC_NAME = name("response_time")
			.operationName("Process Find Form")
			.tag("layer", "All")
			.build();
	private static MetricName.MetricNameTemplate timerMetricNameTemplate = name("response_time")
			.operationName("")
			.layer("All")
			.templateFor("operation_name");
	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private List<MetricName> names = new ArrayList<>();

	int i = 0;

	@Setup
	public void init() {
		i = 0;
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "write")
				.tag("unit", "dropped")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Survivor-Space")
				.tag("type", "committed")
				.build());
		names.add(name("mem_usage")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Compressed-Class-Space")
				.tag("type", "init")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Code-Cache")
				.tag("type", "init")
				.build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "write")
				.tag("unit", "errors")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "write")
				.tag("unit", "dropped")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Metaspace")
				.tag("type", "usage")
				.build());
		names.add(name("cache_size_count")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("jvm_memory_non_heap")
				.tag("type", "init")
				.build());
		names.add(name("disk_usage_percent")
				.tag("mountpoint", "/")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Metaspace")
				.tag("type", "init")
				.build());
		names.add(name("disk_queue")
				.tag("mountpoint", "/")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Eden-Space")
				.tag("type", "max")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "read")
				.tag("unit", "bytes")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "read")
				.tag("unit", "bytes")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "write")
				.tag("unit", "bytes")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Survivor-Space")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Compressed-Class-Space")
				.tag("type", "usage")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "write")
				.tag("unit", "bytes")
				.build());
		names.add(name("jvm_process_cpu_usage").build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Survivor-Space")
				.tag("type", "max")
				.build());
		names.add(name("jvm_memory_total")
				.tag("type", "max")
				.build());
		names.add(name("cpu_info_mhz").build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "write")
				.tag("unit", "bytes")
				.build());
		names.add(name("jvm_memory_total")
				.tag("type", "init")
				.build());
		names.add(name("swap_pages")
				.tag("type", "in")
				.build());
		names.add(name("jvm_gc_time")
				.tag("collector", "PS-MarkSweep")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Eden-Space")
				.tag("type", "used")
				.build());
		names.add(name("cpu_queueLength").build());
		names.add(name("cpu_load")
				.tag("timeframe", "1m")
				.build());
		names.add(name("cache_hit_ratio")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("jvm_gc_count")
				.tag("collector", "PS-Scavenge")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Eden-Space")
				.tag("type", "usage")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "wait")
				.build());
		names.add(name("mem_usage")
				.tag("type", "free")
				.build());
		names.add(name("mem_usage")
				.tag("type", "total")
				.build());
		names.add(name("jvm_memory_non_heap")
				.tag("type", "usage")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "write")
				.tag("unit", "errors")
				.build());
		names.add(name("online").build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "write")
				.tag("unit", "errors")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "read")
				.tag("unit", "errors")
				.build());
		names.add(name("disk_usage")
				.tag("mountpoint", "/")
				.tag("type", "free")
				.build());
		names.add(name("jvm_memory_heap")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Metaspace")
				.tag("type", "max")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "read")
				.tag("unit", "errors")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Old-Gen")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Code-Cache")
				.tag("type", "usage")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "nice")
				.build());
		names.add(name("swap_usage_percent").build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "read")
				.tag("unit", "packets")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Compressed-Class-Space")
				.tag("type", "committed")
				.build());
		names.add(name("swap_usage")
				.tag("type", "free")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Survivor-Space")
				.tag("type", "init")
				.build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "read")
				.tag("unit", "bytes")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "user")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Code-Cache")
				.tag("type", "max")
				.build());
		names.add(name("jvm_memory_non_heap")
				.tag("type", "max")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Code-Cache")
				.tag("type", "committed")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Compressed-Class-Space")
				.tag("type", "max")
				.build());
		names.add(name("cpu_usage_percent").build());
		names.add(name("mem_usage_percent").build());
		names.add(name("cpu_info_cores").build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "write")
				.tag("unit", "packets")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Eden-Space")
				.tag("type", "init")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Survivor-Space")
				.tag("type", "usage")
				.build());
		names.add(name("jvm_memory_heap")
				.tag("type", "usage")
				.build());
		names.add(name("jvm_memory_total")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Metaspace")
				.tag("type", "committed")
				.build());
		names.add(name("swap_pages")
				.tag("type", "out")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Code-Cache")
				.tag("type", "used")
				.build());
		names.add(name("disk_usage")
				.tag("mountpoint", "/")
				.tag("type", "total")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "interrupt")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "soft-interrupt")
				.build());
		names.add(name("cache_size_bytes")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("disk_usage")
				.tag("mountpoint", "/")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_non_heap")
				.tag("type", "committed")
				.build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "read")
				.tag("unit", "dropped")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Old-Gen")
				.tag("type", "init")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "sys")
				.build());
		names.add(name("jvm_memory_heap")
				.tag("type", "init")
				.build());
		names.add(name("swap_usage")
				.tag("type", "total")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "stolen")
				.build());
		names.add(name("jvm_memory_heap")
				.tag("type", "committed")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Old-Gen")
				.tag("type", "usage")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "read")
				.tag("unit", "dropped")
				.build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "read")
				.tag("unit", "errors")
				.build());
		names.add(name("jvm_memory_total")
				.tag("type", "committed")
				.build());
		names.add(name("jvm_gc_count")
				.tag("collector", "PS-MarkSweep")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Compressed-Class-Space")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_non_heap")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_heap")
				.tag("type", "max")
				.build());
		names.add(name("swap_usage")
				.tag("type", "used")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Old-Gen")
				.tag("type", "max")
				.build());
		names.add(name("jvm_gc_time")
				.tag("collector", "PS-Scavenge")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "read")
				.tag("unit", "packets")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "read")
				.tag("unit", "dropped")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "read")
				.tag("unit", "packets")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Eden-Space")
				.tag("type", "committed")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "PS-Old-Gen")
				.tag("type", "committed")
				.build());
		names.add(name("disk_io")
				.tag("mountpoint", "/")
				.tag("type", "write")
				.build());
		names.add(name("disk_io")
				.tag("mountpoint", "/")
				.tag("type", "read")
				.build());
		names.add(name("network_io")
				.tag("ifname", "lo0")
				.tag("type", "write")
				.tag("unit", "dropped")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en0")
				.tag("type", "write")
				.tag("unit", "packets")
				.build());
		names.add(name("network_io")
				.tag("ifname", "en4")
				.tag("type", "write")
				.tag("unit", "packets")
				.build());
		names.add(name("cpu_usage")
				.tag("type", "idle")
				.build());
		names.add(name("jvm_memory_pools")
				.tag("memory_pool", "Metaspace")
				.tag("type", "used")
				.build());
		names.add(name("cpu_info_cache").build());
		names.add(name("request_throughput")
				.operationName("Process Find Form")
				.tag("http_code", "200")
				.build());
		names.add(name("external_requests_rate")
				.operationName("Process Find Form")
				.operationType("jdbc")
				.build());
		names.add(name("rate")
				.tag("signature", "VetController#showVetList")
				.build());
		names.add(name("logging")
				.tag("log_level", "warn")
				.build());
		names.add(name("logging")
				.tag("log_level", "trace")
				.build());
		names.add(name("cache_misses")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("cache_get")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("request_throughput")
				.operationName("All")
				.tag("http_code", "200")
				.build());
		names.add(name("logging")
				.tag("log_level", "info")
				.build());
		names.add(name("request_throughput")
				.operationName("GET /")
				.tag("http_code", "200")
				.build());
		names.add(name("request_throughput")
				.operationName("Init Find Form")
				.tag("http_code", "200")
				.build());
		names.add(name("cache_hits")
				.tag("cache_name", "vets")
				.tag("tier", "All")
				.build());
		names.add(name("request_throughput")
				.operationName("Show Vet List")
				.tag("http_code", "200")
				.build());
		names.add(name("external_requests_rate")
				.operationName("Show Vet List")
				.operationType("jdbc")
				.build());
		names.add(name("logging")
				.tag("log_level", "debug")
				.build());
		names.add(name("external_request_response_time")
				.operationType("jdbc")
				.tag("signature", "All")
				.tag("method", "SELECT")
				.build());
		names.add(name("response_time")
				.operationName("Show Vet List")
				.tag("layer", "All")
				.build());
		names.add(name("response_time_rum")
				.operationName("All")
				.tag("layer", "Page Rendering")
				.build());
		names.add(name("get_jdbc_connection")
				.tag("url", "jdbc:hsqldb:mem:petclinic-SA")
				.build());
		names.add(name("response_time_rum")
				.operationName("All")
				.tag("layer", "Network")
				.build());
		names.add(name("external_request_response_time")
				.operationType("jdbc")
				.tag("signature", "JpaVetRepositoryImpl#findAll")
				.tag("method", "SELECT")
				.build());
		names.add(name("reporting_time")
				.tag("reporter", "elasticsearch")
				.build());
		names.add(name("response_time")
				.operationName("GET /")
				.tag("layer", "All")
				.build());
		names.add(name("response_time")
				.operationName("All")
				.tag("layer", "jdbc")
				.build());
		names.add(name("response_time")
				.operationName("Init Find Form")
				.tag("layer", "All")
				.build());
		names.add(name("response_time_rum")
				.operationName("All")
				.tag("layer", "Server")
				.build());
		names.add(name("timer")
				.tag("signature", "VetController#showVetList")
				.build());
		names.add(name("external_request_response_time")
				.operationType("jdbc")
				.tag("signature", "JpaOwnerRepositoryImpl#findByLastName")
				.tag("method", "SELECT")
				.build());
		names.add(name("response_time_rum")
				.operationName("All")
				.tag("layer", "Dom Processing")
				.build());
		names.add(name("response_time")
				.operationName("All")
				.tag("layer", "All")
				.build());
		names.add(name("response_time_rum")
				.operationName("All")
				.tag("layer", "All")
				.build());
		names.add(METRIC_NAME);
	}

	@Benchmark
	public MetricName buildMetricTemplateSingleValue() {
		i++;
		return timerMetricNameTemplate.build(Integer.toString(i % 100));
	}

	@Benchmark
	public MetricName buildMetricNameSingleValue() {
		i++;
		return name("response_time")
				.operationName(Integer.toString(i % 100))
				.layer("All")
				.build();
	}

	@Benchmark
	public MetricName buildMetricTemplateMultipleValues() {
		i++;
		final String s = Integer.toString(i % 100);
		return externalRequestTemplate.build(s, s, s);
	}

	@Benchmark
	public MetricName buildMetricNameMultipleValues() {
		i++;
		final String s = Integer.toString(i % 100);
		return name("external_request_response_time").type(s).tag("signature", s).tag("method", s).build();
	}

	@Benchmark
	public void matchMetricName(Blackhole bh) {
		for (MetricName name : names) {
			bh.consume(name.matches(METRIC_NAME));
		}
	}

}
