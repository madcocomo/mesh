package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.FieldUtil;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.RoutingContext;

public class MicroschemaContainerTest extends AbstractBasicIsolatedObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			MicroschemaContainer vcard = microschemaContainer("vcard");
			MicroschemaReference reference = vcard.transformToReference();
			assertNotNull(reference);
			assertEquals("vcard", reference.getName());
			assertEquals(vcard.getUuid(), reference.getUuid());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (NoTrx noTx = db.noTrx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);

			int expectedMicroschemaContainers = microschemaContainers().size();

			for (int i = 1; i <= expectedMicroschemaContainers + 1; i++) {
				PageImpl<? extends MicroschemaContainer> page = boot.microschemaContainerRoot().findAll(ac, new PagingParameters(1, i));

				assertEquals(microschemaContainers().size(), page.getTotalElements());
				assertEquals(Math.min(expectedMicroschemaContainers, i), page.getSize());
			}
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTrx noTx = db.noTrx()) {
			String invalidName = "thereIsNoMicroschemaWithThisName";

			for (String name : microschemaContainers().keySet()) {
				MicroschemaContainer container = boot.microschemaContainerRoot().findByName(name).toBlocking().single();
				assertNotNull("Could not find microschema container for name " + name, container);
				Microschema microschema = container.getLatestVersion().getSchema();
				assertNotNull("Container for microschema " + name + " did not contain a microschema", microschema);
				assertEquals("Check microschema name", name, microschema.getName());
			}

			assertNull("Must not find microschema with name " + invalidName,
					boot.microschemaContainerRoot().findByName(invalidName).toBlocking().single());
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (NoTrx noTx = db.noTrx()) {
			String invalidUUID = UUIDUtil.randomUUID();

			MicroschemaContainerRoot root = boot.microschemaContainerRoot();
			for (MicroschemaContainer container : microschemaContainers().values()) {
				String uuid = container.getUuid();
				assertNotNull("Could not find microschema with uuid " + uuid, root.findByUuid(uuid).toBlocking().single());
			}

			assertNull("Must not find microschema with uuid " + invalidUUID, root.findByUuid(invalidUUID).toBlocking().single());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRead() throws IOException {
		fail("Not yet implemented");
	}

	@Test
	public void testRoot() {
		try (NoTrx noTx = db.noTrx()) {
			MicroschemaContainer vcard = microschemaContainer("vcard");
			assertNotNull(vcard.getRoot());
		}
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema schema = new MicroschemaModel();
			schema.setName("test");
			MicroschemaContainer container = MeshRoot.getInstance().getMicroschemaContainerRoot().create(schema, user());
			assertNotNull("The container was not created.", container);
			assertNotNull("The container schema was not set", container.getLatestVersion().getSchema());
			assertEquals("The creator was not set.", user().getUuid(), container.getCreator().getUuid());
		}
	}

	/**
	 * Assert that the schema version is in sync with its rest model.
	 */
	@Test
	public void testVersionSync() {
		try (NoTrx noTx = db.noTrx()) {
			assertNotNull(microschemaContainer("vcard"));
			assertEquals("The microschema container and schema rest model version must always be in sync",
					microschemaContainer("vcard").getLatestVersion().getVersion(),
					microschemaContainer("vcard").getLatestVersion().getSchema().getVersion());
		}

	}

	@Test
	@Override
	public void testDelete() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema schema = new MicroschemaModel();
			schema.setName("test");
			MicroschemaContainer container = MeshRoot.getInstance().getMicroschemaContainerRoot().create(schema, user());
			assertNotNull(MeshRoot.getInstance().getMicroschemaContainerRoot().findByName("test").toBlocking().single());
			SearchQueueBatch batch = createBatch();
			container.delete(batch);
			assertNull(MeshRoot.getInstance().getMicroschemaContainerRoot().findByName("test").toBlocking().single());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema microschema = new MicroschemaModel();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.READ_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema microschema = new MicroschemaModel();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.DELETE_PERM, microschemaContainer);
		}

	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema microschema = new MicroschemaModel();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.UPDATE_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			Microschema microschema = new MicroschemaModel();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.CREATE_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (NoTrx noTx = db.noTrx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);
			MicroschemaContainer vcard = microschemaContainer("vcard");
			Microschema schema = vcard.transformToRest(ac, 0, "en").toBlocking().single();
			assertEquals(vcard.getUuid(), schema.getUuid());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshJsonException {
		try (NoTrx noTx = db.noTrx()) {
			MicroschemaContainerRoot root = meshRoot().getMicroschemaContainerRoot();

			Microschema microschema = new MicroschemaModel();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer container = root.create(microschema, user());

			assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, container));
			getRequestUser().addCRUDPermissionOnRole(meshRoot().getMicroschemaContainerRoot(), GraphPermission.CREATE_PERM, container);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new microschema container.",
					role().hasPermission(GraphPermission.CREATE_PERM, container));
		}
	}

	/**
	 * Test getting NodeGraphFieldContainers that container Micronodes using a specific microschema container version
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetContainerUsingMicroschemaVersion() throws IOException {
		try (NoTrx noTx = db.noTrx()) {
			MicroschemaContainerVersion vcard = microschemaContainer("vcard").getLatestVersion();

			Microschema microschema = vcard.getSchema();
			Microschema updatedMicroschema = new MicroschemaModel();
			updatedMicroschema.setName(microschema.getName());
			updatedMicroschema.getFields().addAll(microschema.getFields());
			updatedMicroschema.addField(FieldUtil.createStringFieldSchema("newfield"));

			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(MicroschemaComparator.getIntance().diff(microschema, updatedMicroschema));

			InternalActionContext ac = getMockedInternalActionContext();
			vcard.applyChanges(ac, model).toBlocking().last();
			MicroschemaContainerVersion newVCard = microschemaContainer("vcard").getLatestVersion();

			NodeGraphFieldContainer containerWithBoth = folder("2015").getGraphFieldContainer("en");
			containerWithBoth.createMicronode("single", vcard);
			containerWithBoth.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithField = folder("news").getGraphFieldContainer("en");
			containerWithField.createMicronode("single", vcard);

			NodeGraphFieldContainer containerWithList = folder("products").getGraphFieldContainer("en");
			containerWithList.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithOtherVersion = folder("deals").getGraphFieldContainer("en");
			containerWithOtherVersion.createMicronode("single", newVCard);

			List<NodeGraphFieldContainer> containers = new ArrayList<>(vcard.getFieldContainers(project().getLatestRelease().getUuid()));
			assertThat(containers).containsOnly(containerWithBoth, containerWithField, containerWithList).hasSize(3);
		}
	}
}