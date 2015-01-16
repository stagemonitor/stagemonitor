package org.stagemonitor.alerting;

import java.util.List;

public interface CheckGroupRepository {

	List<CheckGroup> getAllActiveCheckGroups();
}
