package com.gentics.mesh.core.csdb.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.csdb.NodePopulator;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.field.XmlGraphField;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.XmlFieldSchema;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;

/**
 * Base for all S1000D XML node populators.
 */
public abstract class S1000dXmlNodePopulator implements NodePopulator {
	
	private static final Logger log = LoggerFactory.getLogger(S1000dXmlNodePopulator.class);
	
	private static final String MESSAGE_FIELD_NOT_FOUND = 
		"XML field '{}' not populated for node {} using file {}: "
		+ "a field of type {} is not found in schema {}";
	
	public static final String FIELD_NAME_REFERENCES = "references";
	public static final String FIELD_NAME_CONTENT = "content";

	@Override
	public boolean accept(Path file, Map<String, Object> context) {
		String filename = file.getFileName().toString();
		return Files.isRegularFile(file)
			&& FilenameUtils.getExtension(filename).equalsIgnoreCase("xml");
	}
	
	/**
	 * Populates the specified XML (content) field of the node instance using the supplied XML file.
	 * 
	 * Do nothing if the field is not found or is not compatible.
	 * 
	 * @param file The XML file.
	 * @param node The node instance to populate.
	 * @param fieldName Name of the XML field.
	 */
	protected void populateXmlField(final Path file, final NodeGraphFieldContainer node, final String fieldName) {
		// Check whether the field is defined.
		SchemaModel schema = node.getSchemaContainerVersion().getSchema();
		if (NodePopulator.checkField(schema, fieldName, XmlFieldSchema.class)) {
			log.warn(MESSAGE_FIELD_NOT_FOUND, node.getUuid(), file.getFileName(), 
				fieldName, XmlFieldSchema.class, schema.getName());
			return;
		}
		
		// Create a new XML object.
		Xml xml = MeshInternal.get().boot().xmlRoot().createItem();
		// Extract properties from file and assign to the XML object.
		xml.setSize(FileUtils.size(file.toString()));
		xml.setChecksum(FileUtils.hash(file.toString()));
		xml.setSchemaName("descript.xsd"); // TODO
		xml.setSchemaVariantName("S1000D_4-2"); // TODO
		
		// Write XML content to binary storage.
		AsyncFile asyncFile = Mesh.vertx().fileSystem().openBlocking(file.toString(), new OpenOptions());
		Flowable<Buffer> stream = RxUtil.toBufferFlow(asyncFile);
		Completable storeOp = MeshInternal.get().binaryStorage().store(stream, xml.getUuid());
		
		// Remove the old XML field from this node instance if needed.
		XmlGraphField oldXmlGraphField = node.getXml(fieldName);
		if (oldXmlGraphField != null) {
			oldXmlGraphField.removeField(node);
		}
		
		// Add a new XML field.
		XmlGraphField newXmlField = node.createXml(fieldName, xml);
		newXmlField.setFilename(file.getFileName().toString());
		
		// Wait for storage persistence to complete.
		storeOp.blockingAwait();
	}
}
