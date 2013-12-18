package de.isys.jawap.server.core;

import de.isys.jawap.entities.MeasurementSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementSessionRepository extends JpaRepository<MeasurementSession, Integer> {
}
