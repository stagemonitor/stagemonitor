package de.isys.jawap.server;

import de.isys.jawap.entities.web.HttpRequestContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloController {


	@RequestMapping("/")
	public String index() {
		return "Welcome!";
	}


	@RequestMapping("/hello")
	public String hello() {
		return "Hello!";
	}
}
