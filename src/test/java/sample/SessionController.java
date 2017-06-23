package sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * @author Rob Winch
 * @since 5.0
 */
@RestController
@RequestMapping("/session")
public class SessionController {
	private String attrName = "attrName";

	@GetMapping("/set")
	public Mono<String> set(WebSession session) {
		session.getAttributes().put(attrName, "Hello");
		return Mono.just("Set");
	}

	@GetMapping("/get")
	public Mono<String> get(WebSession session) {
		Optional<String> attribute = session.getAttribute(attrName);
		String value = attribute.get();
		return Mono.justOrEmpty(value);
	}
}
