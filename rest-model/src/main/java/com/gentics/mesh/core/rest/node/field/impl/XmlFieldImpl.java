package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.XmlField;

public class XmlFieldImpl implements XmlField {
	
	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the binary data. Two fields which share the same binary data will also share the same Uuid.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("File name.")
	private String fileName;
	
	@JsonProperty(required = true)
	@JsonPropertyDescription("Size of the file in bytes.")
	private long fileSize;
	
	@JsonProperty(required = true)
	@JsonPropertyDescription("SHA 512 checksum of the file.")
	private String checksum;

	@JsonProperty(required = false)
	@JsonPropertyDescription("XML Schema name of the file.")
	private String xmlSchemaName;

	public String getUuid() {
		return uuid;
	}

	public XmlField setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public XmlField setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public long getFileSize() {
		return fileSize;
	}

	public XmlField setFileSize(long fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public String getChecksum() {
		return checksum;
	}

	public XmlField setChecksum(String checksum) {
		this.checksum = checksum;
		return this;
	}

	public String getXmlSchemaName() {
		return xmlSchemaName;
	}

	public XmlField setXmlSchemaName(String xmlSchemaName) {
		this.xmlSchemaName = xmlSchemaName;
		return this;
	}
	
	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.XML.toString();
	}
}
