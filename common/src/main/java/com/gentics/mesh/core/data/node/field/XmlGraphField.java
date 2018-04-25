package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.core.rest.node.field.XmlField;

public interface XmlGraphField extends BasicGraphField<XmlField>, MeshEdge {

	String XML_FILENAME_PROPERTY_KEY = "xmlFilename";
	
	default String getFilename() {
		return getProperty(XML_FILENAME_PROPERTY_KEY);
	}
	
	default XmlGraphField setFilename(String filename) {
		setProperty(XML_FILENAME_PROPERTY_KEY, filename);
		return this;
	}
	
	Xml getXml();
}
