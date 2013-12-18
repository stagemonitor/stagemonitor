package de.isys.jawap.server.core;

import de.isys.jawap.entities.MeasurementSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/measurementSessions")
public class MeasurementSessionController {

	@Resource
	private MeasurementSessionRepository measurementSessionRepository;

	@RequestMapping(method = GET)
	List<MeasurementSession> getAll() {
		return measurementSessionRepository.findAll();
	}

	@RequestMapping(method = POST)
	ResponseEntity<Void> create(@RequestBody MeasurementSession measurementSession) {
		measurementSession = measurementSessionRepository.save(measurementSession);
		final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/measurementSessions/" + measurementSession.getId()).build().toUri();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(location);
		return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{id}", method = PUT)
	void update(@PathVariable Integer id, @RequestBody MeasurementSession measurementSession) {
		measurementSession.setId(id);
		measurementSessionRepository.save(measurementSession);
	}
}
