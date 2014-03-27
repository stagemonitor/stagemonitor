package org.stagemonitor.server.dashboard

import org.springframework.data.jpa.repository.JpaRepository

public interface DashboardRepository extends JpaRepository<Dashboard, String> {

}