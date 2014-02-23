package de.isys.jawap.entities.profiler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class CallStackLob {

	@Id
	@GeneratedValue
	private Integer id;
	@Lob
	@JsonIgnore
	private String callStackJson;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCallStackJson() {
		return callStackJson;
	}

	public void setCallStackJson(String callStackJson) {
		this.callStackJson = callStackJson;
	}
}
