package de.isys.jawap.server

import de.isys.jawap.entities.MeasurementSession
import de.isys.jawap.entities.profiler.CallStackElement
import de.isys.jawap.entities.web.HttpExecutionContext
import de.isys.jawap.server.core.MeasurementSessionRepository
import de.isys.jawap.server.profiler.HttpExecutionContextRepository

import javax.inject.Inject
import javax.inject.Named


@Named
class TestData {

	@Inject
	private HttpExecutionContextRepository httpExecutionContextRepository

	@Inject
	private MeasurementSessionRepository measurementSessionRepository

	MeasurementSession addMeasurementSession(String app = 'testapp', String host = 'localhorst', String inst='test') {
		MeasurementSession measurementSession = new MeasurementSession();
		measurementSession.setApplicationName(app);
		measurementSession.setHostName(host);
		measurementSession.setInstanceName(inst);
		return measurementSessionRepository.save(measurementSession)
	}

	HttpExecutionContext addHttpExecutionContext(MeasurementSession measurementSession, String name) {
		HttpExecutionContext httpExecutionContext = new HttpExecutionContext();
		httpExecutionContext.setMeasurementSession(measurementSession);
		httpExecutionContext.setName(name);
		httpExecutionContext.setUrl(name);

		CallStackElement callStack = new CallStackElement(null);
		httpExecutionContext.setCallStack(callStack);
		return httpExecutionContextRepository.save(httpExecutionContext);
	}
}
