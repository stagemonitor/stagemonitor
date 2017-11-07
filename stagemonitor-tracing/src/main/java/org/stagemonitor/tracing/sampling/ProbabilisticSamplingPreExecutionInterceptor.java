package org.stagemonitor.tracing.sampling;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProbabilisticSamplingPreExecutionInterceptor extends PreExecutionSpanInterceptor {

	private static final int BIT_SET_SIZE = 100;
	private BitSet defaultSampleDecisions;
	private Map<String, BitSet> sampleDecisionsByType;
	private AtomicInteger spanCounter = new AtomicInteger();
	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry configuration) {
		tracingPlugin = configuration.getConfig(TracingPlugin.class);

		defaultSampleDecisions = getBitSet(tracingPlugin.getDefaultRateLimitSpansPercent());
		setBitSetMap(tracingPlugin.getRateLimitSpansPerMinutePercentPerType());
		handleRuntimeConfigChanges(tracingPlugin);
	}

	private void handleRuntimeConfigChanges(TracingPlugin tracingPlugin) {
		tracingPlugin.getDefaultRateLimitSpansPercentOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				defaultSampleDecisions = getBitSet(newValue);
			}
		});
		tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption().addChangeListener(new ConfigurationOption.ChangeListener<Map<String, Double>>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Map<String, Double> oldValue, Map<String, Double> newValue) {
				setBitSetMap(newValue);
			}
		});
	}

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final SpanContextInformation spanContext = context.getSpanContext();
		final BitSet sampleDecisions;
		final String operationType = spanContext.getOperationType();
		if (sampleDecisionsByType.containsKey(operationType)) {
			sampleDecisions = sampleDecisionsByType.get(operationType);
		} else if (isRoot(context.getSpanContext().getSpanWrapper())) {
			sampleDecisions = defaultSampleDecisions;
		} else {
			return;
		}
		if (sampleDecisions != null && !isSampled(sampleDecisions, spanCounter)) {
			context.shouldNotReport(getClass());
		}
	}

	protected boolean isRoot(SpanWrapper span) {
		return tracingPlugin.isRoot(span);
	}

	private boolean isSampled(BitSet sampleDecisions, AtomicInteger spanCounter) {
		return sampleDecisions.get(Math.abs(spanCounter.getAndIncrement()) % BIT_SET_SIZE);
	}

	private void setBitSetMap(Map<String, Double> newValue) {
		Map<String, BitSet> rateLimiters = new HashMap<String, BitSet>();
		for (Map.Entry<String, Double> entry : newValue.entrySet()) {
			rateLimiters.put(entry.getKey(), getBitSet(entry.getValue()));
		}
		sampleDecisionsByType = rateLimiters;
	}

	private BitSet getBitSet(double probability) {
		// fast-circuit when always sample
		// no need to increment counters and generate a bit set then
		if (probability == 1.0) {
			return null;
		}
		return randomBitSet(BIT_SET_SIZE, (int) (probability * BIT_SET_SIZE), new Random());
	}

	/**
	 * Reservoir sampling algorithm borrowed from Stack Overflow.
	 *
	 * http://stackoverflow.com/questions/12817946/generate-a-random-bitset-with-n-1s
	 */
	private static BitSet randomBitSet(int size, int cardinality, Random rnd) {
		BitSet result = new BitSet(size);
		int[] chosen = new int[cardinality];
		// set first cardinality bits to 1
		// the rest (size - cardinality) is still 0
		int i;
		for (i = 0; i < cardinality; ++i) {
			chosen[i] = i;
			result.set(i);
		}

		// shuffle to more evenly (randomly) distribute 0's and 1's
		for (; i < size; ++i) {
			int j = rnd.nextInt(i + 1);
			if (j < cardinality) {
				result.clear(chosen[j]);
				result.set(i);
				chosen[j] = i;
			}
		}
		return result;
	}

}
