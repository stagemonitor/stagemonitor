package org.stagemonitor.os.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.CachedGauge;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.metrics.metrics2.Metric2Set;

public abstract class AbstractSigarMetricSet<T> implements Metric2Set {

	private final Sigar sigar;

	/*
	 * The CachedGauge is used as a cache, to ensure that a single report gets a single instance of the metric snapshot
	 */
	private CachedGauge<T> sigarMetricSnapshot = new CachedGauge<T>(900, TimeUnit.MILLISECONDS) {
		@Override
		protected T loadValue() {
			try {
				return loadSnapshot(sigar);
			} catch (SigarException e) {
				throw new RuntimeException(e);
			}
		}
	};

	protected AbstractSigarMetricSet(Sigar sigar) {
		this.sigar = sigar;
	}

	abstract T loadSnapshot(Sigar sigar) throws SigarException;

	/**
	 * Returns a snapshot of {@link T}, which is the same for a single report. The next report returns a different snapshot.
	 *
	 * @return a snapshot of {@link T}
	 */
	public T getSnapshot() {
		return sigarMetricSnapshot.getValue();
	}

}
