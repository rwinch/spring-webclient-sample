package sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

@Configuration
@EnableWebFlux
@ComponentScan
public class HelloWebfluxApplication {
	@Value("${server.port:8080}")
	private int port = 8080;

	public static void main(String[] args) throws Exception {
		try(AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(HelloWebfluxApplication.class)) {
			context.getBean(NettyContext.class).onClose().block();
		}
	}

	@Bean
	public NettyContext nettyContext(ApplicationContext context) {
		HttpHandler handler = DispatcherHandler.toHttpHandler(context);
		ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
		HttpServer httpServer = HttpServer.create("localhost", port);
		return httpServer.newHandler(adapter).block();
	}
}