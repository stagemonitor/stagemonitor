package org.stagemonitor.server.core;

import org.stagemonitor.entities.MeasurementSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementSessionRepository extends JpaRepository<MeasurementSession, Integer> {
}
