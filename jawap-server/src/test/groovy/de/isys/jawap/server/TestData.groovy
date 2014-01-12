package de.isys.jawap.server

import de.isys.jawap.entities.MeasurementSession
import de.isys.jawap.entities.profiler.CallStackElement
import de.isys.jawap.entities.web.HttpRequestContext
import de.isys.jawap.server.core.MeasurementSessionRepository
import de.isys.jawap.server.profiler.HttpRequestContextRepository

import javax.inject.Inject
import javax.inject.Named


@Named
class TestData {

	@Inject
	private HttpRequestContextRepository httpRequestContextRepository

	@Inject
	private MeasurementSessionRepository measurementSessionRepository

	MeasurementSession addMeasurementSession(String app = 'testapp', String host = 'localhorst', String env='test') {
		MeasurementSession measurementSession = new MeasurementSession();
		measurementSession.setApplicationName(app);
		measurementSession.setHostName(host);
		measurementSession.setInstanceName(env);
		return measurementSessionRepository.save(measurementSession)
	}

	HttpRequestContext addHttpRequestContext(MeasurementSession measurementSession, String name) {
		HttpRequestContext httpRequestContext = new HttpRequestContext();
		httpRequestContext.setMeasurementSession(measurementSession);
		httpRequestContext.setName(name);
		httpRequestContext.setUrl(name);

		CallStackElement callStack = new CallStackElement(null);
		httpRequestContext.setCallStack(callStack);
		return httpRequestContextRepository.save(httpRequestContext);
	}
}
