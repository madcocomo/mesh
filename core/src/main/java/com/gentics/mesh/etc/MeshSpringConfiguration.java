package com.gentics.mesh.etc;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import javax.annotation.PostConstruct;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.graphdb.DatabaseServiceProvider;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class MeshSpringConfiguration {

	public static MeshSpringConfiguration instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshSpringConfiguration getMeshSpringConfiguration() {
		return instance;
	}

	private static final Logger log = LoggerFactory.getLogger(MeshSpringConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@Bean
	public FramedThreadedTransactionalGraph getFramedThreadedTransactionalGraph() {
		String className = Mesh.mesh().getOptions().getDatabaseProviderClass();
		try {
			Class<?> clazz = Class.forName(className);
			DatabaseServiceProvider provider = (DatabaseServiceProvider) clazz.newInstance();
			JsonObject settings = new JsonObject();
			return provider.getFramedGraph(settings);
		} catch (Exception e) {
			String msg = "Could not load database provider class {" + className + "}. Maybe there is no such provider within the classpath.";
			log.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Bean
	public Node elasticSearchNode() {
		Node node = NodeBuilder.nodeBuilder().node();
		return node;
	}

	@Bean
	public SessionHandler sessionHandler() {
		SessionStore store = LocalSessionStore.create(Mesh.vertx());
		return new SessionHandlerImpl("mesh.session", 30 * 60 * 1000, false, store);
	}

	@Bean
	public AuthHandler authHandler() {
		return BasicAuthHandler.create(authProvider(), BasicAuthHandler.DEFAULT_REALM);
	}

	@Bean
	public UserSessionHandler userSessionHandler() {
		return UserSessionHandler.create(authProvider());
	}

	@Bean
	public AuthProvider authProvider() {
		return new MeshAuthProvider();
	}

	@Bean
	public MailClient mailClient() {
		MailConfig config = Mesh.mesh().getOptions().getMailServerOptions();

		// config.setHostname(options.getHostname());
		// config.setPort(options.getPort());
		// config.setStarttls(StartTLSOptions.REQUIRED);
		// config.setUsername(options.getUsername());
		// config.setPassword(options.getPassword());
		MailClient mailClient = MailClient.createShared(Mesh.vertx(), config, "meshClient");
		return mailClient;
	}

	public CorsHandler corsHandler() {
		String pattern = Mesh.mesh().getOptions().getCorsAllowedOriginPattern();
		CorsHandler corsHandler = CorsHandler.create(pattern);
		corsHandler.allowedMethod(HttpMethod.GET);
		corsHandler.allowedMethod(HttpMethod.POST);
		corsHandler.allowedMethod(HttpMethod.PUT);
		corsHandler.allowedMethod(HttpMethod.DELETE);
		corsHandler.allowedHeader("Authorization");
		corsHandler.allowedHeader("Content-Type");
		return corsHandler;
	}

	// TODO maybe uploads should use a dedicated bodyhandler?
	@Bean
	public Handler<RoutingContext> bodyHandler() {
		BodyHandler handler = BodyHandler.create();
		handler.setBodyLimit(Mesh.mesh().getOptions().getFileUploadByteLimit());
		// TODO check for windows issues
		handler.setUploadsDirectory("target/" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY);
		return handler;
	}

}
