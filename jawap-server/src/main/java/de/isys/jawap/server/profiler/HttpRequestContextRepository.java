package de.isys.jawap.server.profiler;

import de.isys.jawap.entities.web.HttpRequestContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HttpRequestContextRepository extends JpaRepository<HttpRequestContext, Integer> {
}
