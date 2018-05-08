package com.gentics.mesh.core.csdb;

import java.nio.file.Path;
import java.util.Map;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

/**
 * A node populator accepts a file input and attempt to extract and populate the content of a mesh node.
 * 
 * Essential for file upload handling and archive importing.
 * 
 * Each implementation shall focus on handling of a specific type of file.
 */
public interface NodePopulator {
	
	/** Base priority. All general-purpose implementation should have a priority equal or below. */
	public static final int PRIORITY_BASE = 1000;
	
	/**
	 * Checks whether the schema has a field of the specified name and of the compatible type.
	 * 
	 * @param schema The schema.
	 * @param fieldName The name of the schema field.
	 * @param expectedClass The expected class of the field.
	 * 
	 * @return True if it is.
	 */
	static boolean checkField(SchemaModel schema, String fieldName, Class<? extends FieldSchema> expectedClass) {
		FieldSchema contentField = schema.getField(fieldName);
		return (contentField != null && expectedClass.isInstance(contentField));
	}
	
	/**
	 * Gets the priority of the populator implementation.
	 * 
	 * The higher the number, the earlier this populater gets selected during acceptance testing.
	 * 
	 * @return Priority number.
	 */
	int getPriority();

	/**
	 * Checks whether the populator supports this specific file.
	 * 
	 * @param file The file or folder.
	 * @param context A map to set / get properties for / from the other populators.
	 * 
	 * @return True if the populator supports this file and should be used to populate a node.
	 */
	boolean accept(Path file, Map<String, Object> context);
	
	/**
	 * Parses the name of the node (one of the populated properties) only.
	 * 
	 * Node name is used as reference target for the node, therefore the separation of name parsing and rest of the 
	 * content is to solve the chicken-and-egg problem of reference resolution.
	 * 
	 * @param file The file or folder.
	 * 
	 * @return The name of the node.
	 */
	String parseNodeName(Path file);
	
	/**
	 * Populates only the display name field of the node version.
	 * 
	 * The name is supplied via calling parseNodeName().
	 * 
	 * @param file The file or folder.
	 * @param node The node to populate.
	 */
	default void populateDisplayName(Path file, NodeGraphFieldContainer node) {
		String displayName = parseNodeName(file);
		if (!displayName.equals(node.getDisplayFieldValue())) {
			// Get or create the "display name" field.
			SchemaModel schema = node.getSchemaContainerVersion().getSchema();
			// Assert the "display name" field is defined as a string field.
			FieldSchema fieldSchema = schema.getField(schema.getDisplayField());
			if (!(fieldSchema instanceof StringFieldSchema)) {
				throw Errors.internalError("display field {} of schema {} is not a string field", 
					fieldSchema.getName(), schema.getName());
			}
			// Get or create the "display name" field as a string field.
			StringGraphField graphStringField = node.getString(schema.getDisplayField());
			if (graphStringField == null) {
				graphStringField = node.createString(schema.getDisplayField());
			}
			// Set a new value.
			graphStringField.setString(displayName);
			// Update the cached value in the main node too.
			node.updateDisplayFieldValue();
		}
	}
	
	/**
	 * Populates content of the node using information extracted from the specific file.
	 * 
	 * The file will first be tested against this populator via a call to accept(file, context).
	 * 
	 * @param file The file or folder to read from.
	 * @param node The node to write to.
	 */
	void populateContent(Path file, NodeGraphFieldContainer node);
}
