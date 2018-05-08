package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_FOLDER_NODE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobStatusHandler;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This job takes a input archive, extract it, create corresponding nodes in the "file system"
 * starting from a specified "root" node and PUT file content into a specified field of the node.
 * 
 * The user is expected to specify mapping of file extention to a schema type.
 */
public class ArchiveImportJobImpl extends JobImpl {
	
	private static final Logger log = LoggerFactory.getLogger(ArchiveImportJobImpl.class);
	
	public static final String CONTENT_LANGUAGE_PROPERTY_KEY = "contentLanguage";
	
	public static final String ARCHIVE_PATH_PROPERTY_KEY = "archivePath";
	
	public static final String SCHEMA_FOR_FOLDER_PROPERTY_KEY = "schemaForFolder";
	
	public static final String SCHEMA_FOR_XML_PROPERTY_KEY = "schemaForXml";
	
	public static final String SCHEMA_FOR_BINARY_PROPERTY_KEY = "schemaForBinary";

	
	public static void init(Database database) {
		database.addVertexType(ArchiveImportJobImpl.class, MeshVertexImpl.class);
	}
	
	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = super.transformToRestSync(ac, level, languageTags);
		Map<String, String> props = response.getProperties();
		Node rootFolderNode = getRootFolderNode();
		if (rootFolderNode != null) {
			props.put("rootFolderNode", rootFolderNode.getUuid());
		}
		props.put("contentLanguage", ARCHIVE_PATH_PROPERTY_KEY);
		props.put("archivePath", getArchiveFilePath());
		props.put("schemaForFolder", getSchemaForFolder());
		props.put("schemaForXml", getSchemaForXml());
		props.put("schemaForBinary", getSchemaForBinary());
		return response;
	}
	
	/**
	 * Gets the root folder node.
	 * @return The root folder node.
	 */
	public Node getRootFolderNode() {
		return out(HAS_ROOT_FOLDER_NODE)
			.has(NodeImpl.class)
			.nextOrDefaultExplicit(NodeImpl.class, null);
	}

	/**
	 * Sets the root folder node. Must be a container archtype.
	 * @param rootFolderNode The root folder node.
	 */
	public void setRootFolderNode(Node rootFolderNode) {
		setUniqueLinkOutTo(rootFolderNode, HAS_ROOT_FOLDER_NODE);
	}
	
	/**
	 * Gets the path to the (server-side) local archive file to import.
	 * @return The path to the archive file.
	 */
	public String getArchiveFilePath() {
		return getProperty(ARCHIVE_PATH_PROPERTY_KEY);
	}
	
	/**
	 * Sets the path to the (server-side) local archive file to import.
	 * @param archiveFilePath The path to the archive file.
	 */
	public ArchiveImportJobImpl setArchiveFilePath(String archiveFilePath) {
		setProperty(ARCHIVE_PATH_PROPERTY_KEY, archiveFilePath);
		return this;
	}
	
	public String getSchemaForFolder() {
		return getProperty(SCHEMA_FOR_FOLDER_PROPERTY_KEY);
	}
	
	public ArchiveImportJobImpl setSchemaForFolder(String schemaForFolder) {
		setProperty(SCHEMA_FOR_FOLDER_PROPERTY_KEY, schemaForFolder);
		return this;
	}
	
	public String getSchemaForXml() {
		return getProperty(SCHEMA_FOR_XML_PROPERTY_KEY);
	}
	
	public ArchiveImportJobImpl setSchemaForXml(String schemaForXml) {
		setProperty(SCHEMA_FOR_XML_PROPERTY_KEY, schemaForXml);
		return this;
	}
	
	public String getSchemaForBinary() {
		return getProperty(SCHEMA_FOR_BINARY_PROPERTY_KEY);
	}
	
	public ArchiveImportJobImpl setSchemaForBinary(String schemaForBinary) {
		setProperty(SCHEMA_FOR_BINARY_PROPERTY_KEY, schemaForBinary);
		return this;
	}
	
	public String getContentLanguage() {
		return getProperty(CONTENT_LANGUAGE_PROPERTY_KEY);
	}
	
	public ArchiveImportJobImpl setContentLanguage(String contentLanguage) {
		setProperty(CONTENT_LANGUAGE_PROPERTY_KEY, contentLanguage);
		return this;
	}

	@Override
	public void prepare() {}

	@Override
	protected void processTask() {
		JobStatusHandler status = new JobStatusHandlerImpl(this, Mesh.vertx());
		try {
			log.info("Import Archive Job {} started", getUuid());
			status.commit();
			try (Tx tx = DB.get().tx()) {
				// Find referenced schemas.
				Project project = getRelease().getProject();
				SchemaContainer folderSchema = project.getSchemaContainerRoot().findByUuidFailfast(getSchemaForFolder());
				SchemaContainer xmlSchema = project.getSchemaContainerRoot().findByUuidFailfast(getSchemaForXml());
				SchemaContainer binarySchema = project.getSchemaContainerRoot().findByUuidFailfast(getSchemaForBinary());
				Language language = project.getLanguage(getContentLanguage());
				
				MeshInternal.get().archiveImportHandler().importArchive(
					getCreator(), 
					language, 
					getRelease(), 
					getRootFolderNode(), 
					getArchiveFilePath(), 
					(Path path) -> {
						if (Files.isDirectory(path)) {
							return folderSchema;
						} else if (FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("xml")) {
							// TODO: DITAMAP etc?
							return xmlSchema;
						} else {
							return binarySchema;
						}
					},
					status
				);
				status.done();
			}
		} catch (Exception e) {
			status.error(e, "Error executing import job");
		}
	}

}
