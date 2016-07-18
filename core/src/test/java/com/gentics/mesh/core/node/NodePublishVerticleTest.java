package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

public class NodePublishVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

//	@BeforeClass
//	public static void setupOnce() {
//		new RxDebugger().start();
//	}

	/**
	 * Folder /news/2015 is not published. A new node will be created in folder 2015. Publishing the created folder should fail since the parent folder
	 * (/news/2015) is not yet published. This test will also assert that publishing works fine as soon as the parent node is published.
	 */
	@Test
	public void testPublishNodeInUnpublishedContainer() {

		// 1. Take the parent folder offline
		String parentFolderUuid;
		String subFolderUuid;
		try (NoTrx notrx = db.noTrx()) {
			InternalActionContext ac = getMockedInternalActionContext("recursive=true", user());
			Node subFolder = folder("2015");
			Node parentFolder = folder("news");
			parentFolder.publish(ac);
			subFolder.takeOffline(ac);
			subFolderUuid = subFolder.getUuid();
			parentFolderUuid = parentFolder.getUuid();
		}

		assertPublishStatus("Node 2015 should not be published", subFolderUuid, false);
		assertPublishStatus("Node News should be published", parentFolderUuid, true);

		// 2. Create a new node in the folder 2015
		NodeCreateRequest requestA = new NodeCreateRequest();
		requestA.setLanguage("en");
		requestA.setParentNodeUuid(subFolderUuid);
		requestA.setSchema(new SchemaReference().setName("content"));
		requestA.getFields().put("name", FieldUtil.createStringField("nodeA"));
		requestA.getFields().put("filename", FieldUtil.createStringField("nodeA"));
		NodeResponse nodeA = call(() -> getClient().createNode(PROJECT_NAME, requestA));

		// 3. Publish the created node - It should fail since the parentfolder is not published
		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()), BAD_REQUEST, "node_error_parent_containers_not_published", subFolderUuid);

		// 4. Publish the parent folder
		call(() -> getClient().publishNode(PROJECT_NAME, subFolderUuid));

		// 4. Verify that publishing now works
		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()));

	}

	@Test
	public void testGetPublishStatusForEmptyLanguage() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");
			call(() -> getClient().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "fr"), NOT_FOUND, "error_language_not_found", "fr");
		}
	}

	@Test
	public void testPublishNode() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			PublishStatusResponse statusResponse = call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));
			assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");
		}
	}

	@Test
	public void testGetPublishStatus() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			// 1. Check initial status
			PublishStatusResponse publishStatus = call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Initial publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");

			// 2. Take node offline
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));

			// 3. Assert that node is offline
			publishStatus = call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Publish status after take offline").isNotNull().isNotPublished("en").hasVersion("en", "1.0");

			// 4. Publish the node
			call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));

			// 5. Assert that node has been published
			publishStatus = call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Publish status after publish").isNotNull().isPublished("en").hasVersion("en", "2.0");
		}
	}

	@Test
	public void testGetPublishStatusForRelease() {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update));
			call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));

			PublishStatusResponse publishStatus = call(
					() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParameters().setRelease(initialRelease.getName())));
			assertThat(publishStatus).as("Initial release publish status").isNotNull().isPublished("en").hasVersion("en", "1.0").doesNotContain("de");

			publishStatus = call(
					() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParameters().setRelease(newRelease.getName())));
			assertThat(publishStatus).as("New release publish status").isNotNull().isPublished("de").hasVersion("de", "1.0").doesNotContain("en");

			publishStatus = call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid, new NodeParameters()));
			assertThat(publishStatus).as("New release publish status").isNotNull().isPublished("de").hasVersion("de", "1.0").doesNotContain("en");
		}
	}

	@Test
	public void testGetPublishStatusNoPermission() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("news");
			String nodeUuid = node.getUuid();
			role().revokePermissions(node, READ_PERM);

			call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testGetPublishStatusBogusUuid() {
		String bogusUuid = "bogus";
		call(() -> getClient().getNodePublishStatus(PROJECT_NAME, bogusUuid), NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	public void testGetPublishStatusForLanguage() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("products");

			// 1. Take everything offline
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));

			// 2. Publish only a specific language of a node
			call(() -> getClient().publishNodeLanguage(PROJECT_NAME, node.getUuid(), "en"));

			// 3. Assert that the other language is not published
			assertThat(call(() -> getClient().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "de"))).as("German publish status")
					.isNotPublished();
			assertThat(call(() -> getClient().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "en"))).as("English publish status")
					.isPublished();
		}
	}

	@Test
	public void testPublishNodeForRelease() {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			project.getReleaseRoot().create("newrelease", user());
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update));

			// publish for the initial release
			PublishStatusResponse publishStatus = call(
					() -> getClient().publishNode(PROJECT_NAME, nodeUuid, new VersioningParameters().setRelease(initialRelease.getName())));
			assertThat(publishStatus).as("Initial publish status").isPublished("en").hasVersion("en", "1.0").doesNotContain("de");
		}
	}

	@Test
	public void testPublishNodeNoPermission() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			role().revokePermissions(node, PUBLISH_PERM);

			call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testPublishNodeBogusUuid() {
		String bogusUuid = "bogus";
		call(() -> getClient().publishNode(PROJECT_NAME, bogusUuid), NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	public void testRepublishUnchanged() {
		String nodeUuid = db.noTrx(() -> folder("2015").getUuid());
		PublishStatusResponse statusResponse = call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));
		assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");

		statusResponse = call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));
		assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");
	}

	/**
	 * Verify that the move action fails if the published node is moved into offline containers.
	 */
	@Test
	public void testMoveConsistency() {
		// 1. Take the target folder offline
		String newsFolderUuid = db.noTrx(() -> folder("news").getUuid());
		call(() -> getClient().takeNodeOffline(PROJECT_NAME, newsFolderUuid, new PublishParameters().setRecursive(true)));

		// 2. Move the published node into the offline target node
		String publishedNode = db.noTrx(() -> content("concorde").getUuid());
		call(() -> getClient().moveNode(PROJECT_NAME, publishedNode, newsFolderUuid), BAD_REQUEST, "node_error_parent_containers_not_published", newsFolderUuid);
	}

	@Test
	public void testPublishLanguage() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			// Only publish the test node. Take all children offline
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));
			call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));

			// Update german language -> new draft
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("changed-de"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update));

			assertThat(
					call(() -> getClient().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParameters().setLanguages("de"))).getAvailableLanguages())
							.containsOnly("en");

			// Take english language offline
			call(() -> getClient().takeNodeLanguageOffline(PROJECT_NAME, node.getUuid(), "en"));

			// The node should not be loadable since both languages are offline
			call(() -> getClient().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParameters().setLanguages("de")), NOT_FOUND,
					"node_error_published_not_found_for_uuid_release_version", nodeUuid, project().getLatestRelease().getUuid());

			// Publish german version
			PublishStatusModel publishStatus = call(() -> getClient().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"));
			assertThat(publishStatus).as("Publish status").isPublished().hasVersion("1.0");

			// Assert that german is published and english is offline
			assertThat(
					call(() -> getClient().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParameters().setLanguages("de"))).getAvailableLanguages())
							.containsOnly("de");
			assertThat(call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isPublished("de")
					.hasVersion("de", "1.0").isNotPublished("en").hasVersion("en", "2.0");
		}
	}

	@Test
	public void testPublishEmptyLanguage() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			call(() -> getClient().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"), NOT_FOUND, "error_language_not_found", "de");
		}
	}

	@Test
	public void testPublishLanguageForRelease() {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(),
					new VersioningParameters().setRelease(initialRelease.getUuid()), new PublishParameters().setRecursive(true)));

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015 de"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParameters().setRelease(initialRelease.getName())));

			update.getFields().put("name", FieldUtil.createStringField("2015 new de"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParameters().setRelease(newRelease.getName())));
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("2015 new en"));
			call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParameters().setRelease(newRelease.getName())));

			PublishStatusModel publishStatus = call(() -> getClient().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de",
					new VersioningParameters().setRelease(initialRelease.getName())));
			assertThat(publishStatus).isPublished();

			assertThat(call(
					() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParameters().setRelease(initialRelease.getName()))))
							.as("Initial Release Publish Status").isPublished("de").isNotPublished("en");
			assertThat(
					call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParameters().setRelease(newRelease.getName()))))
							.as("New Release Publish Status").isNotPublished("de").isNotPublished("en");
		}
	}

	@Test
	public void testPublishLanguageNoPermission() {
		try (NoTrx noTx = db.noTrx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			role().revokePermissions(node, PUBLISH_PERM);

			call(() -> getClient().publishNodeLanguage(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testPublishInOfflineContainer() {
		String nodeUuid = db.noTrx(() -> folder("2015").getUuid());

		// 1. Take a node subtree offline
		call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));

		// 2. Try to publish a node from within that subtree structure
		String contentUuid = db.noTrx(() -> content("news_2015").getUuid());
		call(() -> getClient().publishNode(PROJECT_NAME, contentUuid), BAD_REQUEST, "node_error_parent_containers_not_published", nodeUuid);

	}

	@Test
	public void testPublishRecursively() {
		String nodeUuid = db.noTrx(() -> project().getBaseNode().getUuid());
		String contentUuid = db.noTrx(() -> content("news_2015").getUuid());

		// 1. Check initial status
		assertPublishStatus("Node should be published.", nodeUuid, true);
		assertPublishStatus("Node should be published.", contentUuid, true);

		// 2. Take all nodes offline
		call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));
		assertPublishStatus("Node should be offline.", nodeUuid, false);
		assertPublishStatus("Node should be offline.", contentUuid, false);

		// 3. Publish all nodes again 
		call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));
		assertPublishStatus("Node should be online again.", nodeUuid, true);
		assertPublishStatus("Node should be online again.", contentUuid, true);
	}

	@Test
	public void testPublishNoRecursion() {
		String nodeUuid = db.noTrx(() -> project().getBaseNode().getUuid());
		String contentUuid = db.noTrx(() -> content("news_2015").getUuid());

		// 1. Check initial status
		assertPublishStatus("Node should be published.", nodeUuid, true);
		assertPublishStatus("Node should be published.", contentUuid, true);

		// 2. Take all nodes offline
		call(() -> getClient().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParameters().setRecursive(true)));
		assertPublishStatus("Node should be offline.", nodeUuid, false);
		assertPublishStatus("Node should be offline.", contentUuid, false);

		// 3. Publish all nodes again 
		call(() -> getClient().publishNode(PROJECT_NAME, nodeUuid));
		assertPublishStatus("Node should be online again.", nodeUuid, true);
		assertPublishStatus("Sub node should still be offline.", contentUuid, false);
	}

	private void assertPublishStatus(String message, String nodeUuid, boolean expectPublished) {
		PublishStatusResponse initialStatus = call(() -> getClient().getNodePublishStatus(PROJECT_NAME, nodeUuid));
		for (Entry<String, PublishStatusModel> entry : initialStatus.getAvailableLanguages().entrySet()) {
			if (expectPublished != entry.getValue().isPublished()) {
				fail("Publish status check for node {" + nodeUuid + "} failed for language {" + entry.getKey() + "} [" + message + "]");
			}
		}
	}

}