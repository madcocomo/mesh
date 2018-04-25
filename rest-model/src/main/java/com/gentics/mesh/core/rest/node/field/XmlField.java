package com.gentics.mesh.core.rest.node.field;

/**
 * An XML field is a field for storing XML data.
 */
public interface XmlField extends Field {

	String getUuid();
	XmlField setUuid(String uuid);
	long getFileSize();
	XmlField setFileSize(long fileSize);
	String getFileName();
	XmlField setFileName(String fileName);
	String getChecksum();
	XmlField setChecksum(String checksum);
	String getXmlSchemaName();
	XmlField setXmlSchemaName(String schemaName);
}
