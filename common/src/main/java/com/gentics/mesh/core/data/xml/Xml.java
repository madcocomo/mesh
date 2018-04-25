package com.gentics.mesh.core.data.xml;

import com.gentics.mesh.core.data.MeshVertex;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Vertex that contains actual information about the XML content.
 */
public interface Xml extends MeshVertex {
	
	String XML_CHECKSUM_PROPERTY_KEY = "xmlChecksum";
	
	String XML_FILESIZE_KEY = "xmlFileSize";
	
	String XML_SCHEMA_NAME_KEY = "xmlSchemaName";
	
	String XML_SCHEMA_VARIANT_NAME_KEY = "xmlSchemaVariantName";

	/**
	 * Returns the actual data stream.
	 * 
	 * @return The actual data stream.
	 */
	Flowable<Buffer> getStream();
	
	/**
	 * Gets the checksum of the content.
	 * 
	 * @return The checksum as a lower-case hex string.
	 */
	default String getChecksum() {
		return getProperty(XML_CHECKSUM_PROPERTY_KEY);
	}

	/**
	 * Sets the checksum of the content.
	 * 
	 * @param checksum The checksum.
	 * @return This.
	 */
	default Xml setChecksum(String checksum) {
		setProperty(XML_CHECKSUM_PROPERTY_KEY, checksum);
		return this;
	}
	
	/**
	 * Gets the content size in bytes.
	 * 
	 * @return This.
	 */
	default long getSize() {
		Long size = getProperty(XML_FILESIZE_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Sets the content size in bytes
	 * 
	 * @param sizeInBytes Size in bytes.
	 * @return This.
	 */
	default Xml setSize(long sizeInBytes) {
		setProperty(XML_FILESIZE_KEY, sizeInBytes);
		return this;
	}
	
	/**
	 * Gets the XML schema (file) name.
	 * 
	 * @return The XML schema (file) name.
	 */
	default String getSchemaName() {
		return getProperty(XML_SCHEMA_NAME_KEY);
	}
	
	/**
	 * Sets the XML schema (file) name, e.g. "descript.xsd".
	 * 
	 * @param schemaName XML schema (file) name.
	 * 
	 * @return This.
	 */
	default Xml setSchemaName(String schemaName) {
		setProperty(XML_SCHEMA_NAME_KEY, schemaName);
		return this;
	}
	
	/**
	 * Gets the XML schema variant name, e.g. "S1000D-4.2".
	 * 
	 * @return The XML schema variant name.
	 */
	default String getSchemaVariantName() {
		return getProperty(XML_SCHEMA_VARIANT_NAME_KEY);
	}
	
	/**
	 * Sets the XML schema variant name, e.g. "S1000D-4.2".
	 * 
	 * @param schemaVariantName XML schema variant name.
	 * 
	 * @return This.
	 */
	default Xml setSchemaVariantName(String schemaVariantName) {
		setProperty(XML_SCHEMA_VARIANT_NAME_KEY, schemaVariantName);
		return this;
	}
}
