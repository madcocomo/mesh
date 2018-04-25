package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.XmlFieldSchema;

public class XmlFieldSchemaImpl extends AbstractFieldSchema implements XmlFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.XML.toString();
	}
}
