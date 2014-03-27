package org.stagemonitor.server.dashboard

import groovy.transform.Canonical

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
@Canonical
class Dashboard {
	@Id String name
	@Lob String content
}
