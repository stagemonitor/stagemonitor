package de.isys.jawap.server.profiler;

import de.isys.jawap.entities.web.HttpExecutionContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HttpExecutionContextRepository extends JpaRepository<HttpExecutionContext, Integer> {
}
