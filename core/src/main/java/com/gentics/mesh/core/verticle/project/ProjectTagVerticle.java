package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.verticle.handler.TagCRUDHandler;

/**
 * The tag verticle provides rest endpoints which allow manipulation and handling of tag related objects.
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectTagVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectTagVerticle.class);

	@Autowired
	private TagCRUDHandler crudHandler;

	public ProjectTagVerticle() {
		super("tags");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addTaggedNodesHandler();
		if (log.isDebugEnabled()) {
			log.debug("Registered tag verticle endpoints");
		}
	}

	private void addTaggedNodesHandler() {
		Route getRoute = route("/:uuid/nodes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {

			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getTagRoot(), rh -> {
				Tag tag = rh.result();
				Page<? extends Node> page = tag.findTaggedNodes(getUser(rc), getSelectedLanguageTags(rc), getPagingInfo(rc));
				transformAndResponde(rc, page, new NodeListResponse());
			});

		});
	}

	// TODO fetch project specific tag
	// TODO update other fields as well?
	// TODO Update user information
	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleUpdate(rc);
		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO maybe projects should not be a set?
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(rc);
		});
	}

	// TODO filtering, sorting
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleRead(rc);
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			crudHandler.handleReadList(rc);
		});

	}

	// TODO filter by projectName
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleDelete(rc);
		});
	}

}
