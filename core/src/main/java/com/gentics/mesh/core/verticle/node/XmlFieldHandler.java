package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.XmlGraphField;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.XmlFieldSchema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.storage.BinaryStorage;

import dagger.Lazy;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class XmlFieldHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(XmlFieldHandler.class);
	
	private Database db;
	
	private Lazy<BootstrapInitializer> boot;
	
	private SearchQueue searchQueue;
	
	private BinaryStorage binaryStorage;

	@Inject
	public XmlFieldHandler(Database db, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue, BinaryStorage binaryStorage) {
		super();
		this.db = db;
		this.boot = boot;
		this.searchQueue = searchQueue;
		this.binaryStorage = binaryStorage;
	}
	
	/**
	 * Handle reading of an XML field.
	 * 
	 * @param rc Vertex routing context.
	 * @param nodeUuid UUID of the parent node.
	 * @param fieldName Name of the XML field.
	 */
	public void handleGet(RoutingContext rc, String nodeUuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.asyncTx(() -> {
			Project project = ac.getProject();
			// Get parent node.
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, READ_PUBLISHED_PERM);
			// Get the node instance of the active language and release.
			Release release = ac.getRelease(node.getProject());
			NodeGraphFieldContainer nodeContainer = node.findVersion(
				ac.getNodeParameters().getLanguageList(), 
				release.getUuid(),
				ac.getVersioningParameters().getVersion()
			);
			// Assert node instance exists.
			if (nodeContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			// Get the field.
			XmlGraphField xmlGraphField = nodeContainer.getXml(fieldName);
			// Assert field exists.
			if (xmlGraphField == null) {
				throw error(NOT_FOUND, "error_xmlfield_not_found_with_name", fieldName);
			}
			// Get the stored XML object.
			Xml xml = xmlGraphField.getXml();
			// Assert the content is present in the file storage.
			if (!binaryStorage.exists(xml.getUuid())) {
				throw error(NOT_FOUND, "node_error_xml_data_not_found", xml.getUuid());
			}
			HttpServerResponse response = rc.response();
			response.putHeader(HttpHeaders.CONTENT_TYPE, "text/xml");
			response.putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(xml.getSize()));
			response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
			response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
			response.putHeader("content-disposition", "attachment; filename=" + xmlGraphField.getFilename());
			xml.getStream().subscribe(response::write, rc::fail, response::end);
		}).doOnError(ac::fail).subscribe();
	}
	
	public void handlePost(RoutingContext rc, String nodeUuid, String fieldName, String language, String version) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");
		
		final Path uploadFile = extractUploadFile(rc);
		log.info("Updating the XML field '{}' of node {}", fieldName, nodeUuid);
		
		db.tx(() -> {
			Project project = ac.getProject();
			Language languageObj = null;
			if (language != null) {
				// Load assigned language.
				languageObj = boot.get().languageRoot().findByLanguageTag(language);
			} else {
				// Load default language.
				List<? extends Language> languageObjList = project.getLanguages();
				if (languageObjList != null && languageObjList.isEmpty()) {
					languageObj = languageObjList.get(0);
				}
			}
			if (languageObj == null) {
				throw error(BAD_REQUEST, "error_language_not_set");
			}
			Release releaseObj = ac.getRelease();
			Node node = project.getNodeRoot().loadObjectByUuid(ac, nodeUuid, UPDATE_PERM);
			NodeGraphFieldContainer latestDraftVersion = node.getGraphFieldContainer(languageObj, releaseObj, ContainerType.DRAFT);
			if (latestDraftVersion == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", version, language);
			}
			
			// TODO Conflict detection.
			
			// Assert the target field is valid.
			FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if (!(fieldSchema instanceof XmlFieldSchema)) {
				throw error(BAD_REQUEST, "error_found_field_is_not_xml", fieldName);
			}
			
			// Create a new draft version for the node.
			NodeGraphFieldContainer newDraftVersion = node.createGraphFieldContainer(languageObj, releaseObj, ac.getUser(), latestDraftVersion, true);
			
			// Let the populator do its magic /s.
			MeshInternal.get().nodePopulatorService().populateNode(uploadFile, newDraftVersion);
			
			// Submit a search index update query.
			SearchQueueBatch batch = searchQueue.create();
			return batch.store(node, releaseObj.getUuid(), DRAFT, false)
				.processAsync()
				.andThen(node.transformToRest(ac, 0));
		}).subscribe(model -> ac.send(model, HttpResponseStatus.CREATED), ac::fail);
	}
	
	private Path extractUploadFile(RoutingContext rc) {
		// Get the file from either the in-memory buffer or the stored upload file.
		String filePath = rc.remove("__UPLOADED_BODY__");
		if (filePath == null) {
			try {
				Path tempFile = Files.createTempFile("upload", ".bin");
				// Write the entire buffer to that file.
				Buffer buffer = rc.getBody();
				try (OutputStream os = Files.newOutputStream(tempFile)) {
					for (int i = 0; i < buffer.length(); i++) {
						os.write(buffer.getByte(i));
					}
				}
				return tempFile;
			} catch (IOException e) {
				throw new RuntimeException("error saving upload file to " + filePath, e);
			}
		} else {
			return Paths.get(filePath);
		}
	}
}
