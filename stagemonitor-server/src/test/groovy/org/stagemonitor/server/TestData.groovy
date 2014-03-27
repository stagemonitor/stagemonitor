package org.stagemonitor.server

import org.stagemonitor.entities.MeasurementSession
import org.stagemonitor.entities.profiler.CallStackElement
import org.stagemonitor.entities.web.HttpExecutionContext
import org.stagemonitor.server.core.MeasurementSessionRepository
import org.stagemonitor.server.profiler.HttpExecutionContextRepository

import javax.inject.Inject
import javax.inject.Named


@Named
class TestData {

	@Inject
	private HttpExecutionContextRepository httpExecutionContextRepository

	@Inject
	private MeasurementSessionRepository measurementSessionRepository

	MeasurementSession addMeasurementSession(String app = 'testapp', String host = 'localhorst', String inst='test') {
		MeasurementSession measurementSession = new MeasurementSession()
		measurementSession.setApplicationName(app)
		measurementSession.setHostName(host)
		measurementSession.setInstanceName(inst)
		return measurementSessionRepository.save(measurementSession)
	}

	HttpExecutionContext addHttpExecutionContext(MeasurementSession measurementSession, String name) {
		HttpExecutionContext httpExecutionContext = new HttpExecutionContext()
		httpExecutionContext.setMeasurementSession(measurementSession)
		httpExecutionContext.setName(name)
		httpExecutionContext.setUrl(name)

		CallStackElement callStack = new CallStackElement(null)
		httpExecutionContext.setCallStack(callStack)
		httpExecutionContext.convertCallStackToLob()
		return httpExecutionContextRepository.save(httpExecutionContext)
	}
}
