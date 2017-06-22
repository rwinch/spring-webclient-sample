package sample;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * @author Rob Winch
 * @since 5.0
 */
@RestController
public class MakesRestfulCallController {
	private final WebClient client;

	public MakesRestfulCallController(WebClient client) {
		this.client = client;
	}

	@GetMapping("/")
	public Mono<String> makeRestfulCallWithCurrentUserCredentials(@AuthenticationPrincipal UserDetails user) {
		return client
				.mutate()
				// pass the credentials of this particular user in the request for authentication of this specific user
				.filter(basicAuthentication(user.getUsername(),user.getPassword()))
				.build()
				.get()
				.uri("/")
				.retrieve()
				.toEntity(String.class)
				.map( r -> r.getBody());
	}
}
