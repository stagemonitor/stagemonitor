package org.stagemonitor.server

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
public class IndexController {

	@RequestMapping("/")
	public String index() {
		return "redirect:graphitus/dashboard.html?id=request";
	}


}
