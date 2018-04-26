package com.gentics.mesh.auth;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.http.MeshHeaders;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * This class extends the Vert.x AuthHandler, so that it also works when the token is set as a cookie.
 * 
 * Central authentication handler for mesh. All requests to secured resources must pass this handler.
 */
@Singleton
public class MeshAuthHandler extends AuthHandlerImpl implements JWTAuthHandler {

	private static final Logger log = LoggerFactory.getLogger(JWTAuthHandlerImpl.class);

	private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	public static final String ANONYMOUS_USERNAME = "anonymous";

	private final JsonObject options;

	private MeshAuthProvider authProvider;

	private BootstrapInitializer boot;

	private Database database;

	@Inject
	public MeshAuthHandler(MeshAuthProvider authProvider, BootstrapInitializer boot, Database database) {
		super(authProvider);
		this.authProvider = authProvider;
		this.boot = boot;
		this.database = database;
		options = new JsonObject();
	}

	@Override
	public JWTAuthHandler setAudience(List<String> audience) {
		options.put("audience", new JsonArray(audience));
		return this;
	}

	@Override
	public JWTAuthHandler setIssuer(String issuer) {
		options.put("issuer", issuer);
		return this;
	}

	@Override
	public JWTAuthHandler setIgnoreExpiration(boolean ignoreExpiration) {
		options.put("ignoreExpiration", ignoreExpiration);
		return this;
	}

	@Override
	public void handle(RoutingContext context) {

		// 1. Check whether the user is already authenticated
		User user = context.user();
		if (user != null) {
			// Already authenticated in, just authorise
			authorizeUser(user, context);
			return;
		}

		handleJWTAuth(context);

	}

	@Override
	public void parseCredentials(RoutingContext arg0, Handler<AsyncResult<JsonObject>> arg1) {
		// Not needed for this handler
	}

	private void authorizeUser(User user, RoutingContext ctx) {
		authorize(user, authZ -> {
			if (authZ.failed()) {
				ctx.fail(authZ.cause());
				return;
			}
			// success, allowed to continue
			ctx.next();
		});
	}

	/**
	 * Handle the JWT authentication part.
	 * 
	 * @param context
	 */
	private void handleJWTAuth(RoutingContext context) {

		// Extract token from:
		// 1) Query parameter "auth"
		// 2) Cookie "mesh.token"
		// 3) Header "Authorization"
		String token = null;
		
		final HttpServerRequest request = context.request();
		
		// Try query param first.
		token = request.getParam("auth");
		
		// Try cookie.
		// Mesh accepts JWT tokens via the cookie as well in order to handle JWT even for regular HTTP Download requests (eg. non ajax requests (static file
		// downloads)).
		// Store the found token value into the authentication header value. This will effectively overwrite the AUTHORIZATION header value.
		Cookie tokenCookie = context.getCookie(MeshAuthProvider.TOKEN_COOKIE_KEY);
		if (token == null && tokenCookie != null) {
			token = tokenCookie.getValue();
		}

		// Try to load the token from the cookie or AUTHORIZATION header.
		final String authorizationHeader = request.headers().get(AUTHORIZATION);
		if (token == null && authorizationHeader != null) {
			String[] parts = authorizationHeader.split(" ");
			if (parts.length == 2) {
				final String scheme = parts[0], credentials = parts[1];
				
				if (BEARER.matcher(scheme).matches()) {
					token = credentials;
				}
			} else {
				log.warn("Format is Authorization: Bearer [token]");
				context.fail(401);
				return;
			}
		}
		
		// If no token is found, use anonymous auth if enabled.
		final boolean allowAnon = Mesh.mesh().getOptions().getAuthenticationOptions().isEnableAnonymousAccess();
		if (token == null) {
			if (allowAnon) {
				// Check whether the Anonymous-Authentication header was set to disable. This will disable the anonymous authentication method altogether.
				String anonymousAuthHeaderValue = request.headers().get(MeshHeaders.ANONYMOUS_AUTHENTICATION);
				if ("disable".equals(anonymousAuthHeaderValue)) {
					handle401(context);
					return;
				}
				if (log.isDebugEnabled()) {
					log.debug("Using anonymous user.");
				}
				MeshAuthUser anonymousUser = database.tx(() -> boot.userRoot().findMeshAuthUserByUsername(ANONYMOUS_USERNAME));
				if (anonymousUser == null) {
					if (log.isDebugEnabled()) {
						log.debug("No anonymous user and authorization header was found. Can't authenticate request.");
					}
					handle401(context);
					return;
				} else {
					context.setUser(anonymousUser);
					authorizeUser(anonymousUser, context);
					return;
				}
			} else {
				log.warn("No Authorization token value was found");
				handle401(context);
				return;
			}
		}

		// 4. Authenticate the found token using JWT
		JsonObject authInfo = new JsonObject().put("jwt", token).put("options", options);
		authProvider.authenticateJWT(authInfo, res -> {

			// Authentication was successful. Lets update the token cookie to keep it alive
			if (res.succeeded()) {
				AuthenticationResult result = res.result();
				User authenticatedUser = result.getUser();
				context.setUser(authenticatedUser);

				if (!result.isUsingAPIKey()) {
					String jwtToken = authProvider.generateToken(authenticatedUser);
					// Remove the original cookie and set the new one
					context.removeCookie(MeshAuthProvider.TOKEN_COOKIE_KEY);
					context.addCookie(Cookie.cookie(MeshAuthProvider.TOKEN_COOKIE_KEY, jwtToken)
							.setMaxAge(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()).setPath("/"));
				}
				authorizeUser(authenticatedUser, context);
			} else {
				log.warn("JWT decode failure", res.cause());
				handle401(context);
			}
		});
	}

	private void handle401(RoutingContext context) {
		context.fail(401);
	}

}
