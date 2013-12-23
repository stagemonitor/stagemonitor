package de.isys.jawap.server

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
