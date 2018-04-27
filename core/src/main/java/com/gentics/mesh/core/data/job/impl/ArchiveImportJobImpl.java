package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_FOLDER_NODE;

import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobStatusHandler;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This job takes a input archive, extract it, create corresponding nodes in the "file system"
 * starting from a specified "root" node and PUT file content into a specified field of the node.
 * 
 * The user is expected to specify mapping of file extention to a project schema type.
 */
public class ArchiveImportJobImpl extends JobImpl {
	
	private static final Logger log = LoggerFactory.getLogger(ArchiveImportJobImpl.class);
	
	public static void init(Database database) {
		database.addVertexType(ArchiveImportJobImpl.class, MeshVertexImpl.class);
	}
	
	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = super.transformToRestSync(ac, level, languageTags);
		Map<String, String> props = response.getProperties();
		if (getRootFolderNode() != null) {
			Node rootFolderNode = getRootFolderNode();
			props.put("rootFolderNode", rootFolderNode.getUuid());
		}
		return response;
	}
	
	public Node getRootFolderNode() {
		return out(HAS_ROOT_FOLDER_NODE)
			.has(NodeImpl.class)
			.nextOrDefaultExplicit(NodeImpl.class, null);
	}

	public void setRootFolderNode(Node rootFolderNode) {
		setUniqueLinkOutTo(rootFolderNode, HAS_ROOT_FOLDER_NODE);
	}

	@Override
	public void prepare() {}

	@Override
	protected void processTask() {
		JobStatusHandler status = new JobStatusHandlerImpl(this, Mesh.vertx());
		try (Tx tx = DB.get().tx()) {
			status.commit();
			
			// TODO
			
			status.done();
		} catch (Exception e) {
			status.error(e, "Error executing import job");
		}
	}
	
	

}
