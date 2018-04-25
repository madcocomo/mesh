package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.XmlGraphField;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.core.data.xml.impl.XmlImpl;
import com.gentics.mesh.core.rest.node.field.XmlField;
import com.gentics.mesh.core.rest.node.field.impl.XmlFieldImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.handler.ActionContext;

public class XmlGraphFieldImpl extends MeshEdgeImpl implements XmlGraphField {
	
	public static FieldTransformer<XmlField> TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		XmlGraphField graphField = container.getXml(fieldKey);
		if (graphField == null) {
			return null;
		} else {
			return graphField.transformToRest(ac);
		}
	};
	
	public static FieldUpdater UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, fieldSchemaContainer) -> {
		XmlGraphField xmlGraphField = container.getXml(fieldKey);
		XmlField xmlField = fieldMap.getXmlField(fieldKey);
		// Delete graph model
		if (fieldMap.hasField(fieldKey) && xmlField == null && xmlGraphField != null) {
			xmlGraphField.removeField(container);
			return;
		}
		// Do nothing if REST model is null.
		if (xmlField == null) {
			return;
		}
		
		// Set file name.
		if (xmlField.getFileName() != null) {
			// Create missing graph field if needed.
			if (xmlGraphField == null) {
				Xml xml = MeshInternal.get().boot().xmlRoot().findByUuid(xmlField.getUuid());
				xmlGraphField = container.createXml(fieldKey, xml);
			}
			xmlGraphField.setFilename(xmlField.getFileName());
		}
	};
	
	public static FieldGetter GETTER = (container, fieldSchema) -> {
		return container.getXml(fieldSchema.getName());
	};

	@Override
	public XmlField transformToRest(ActionContext ac) {
		XmlField restModel = new XmlFieldImpl();
		restModel.setFileName(getFilename());
		
		Xml xml = getXml();
		if (xml != null) {
			restModel.setUuid(xml.getUuid());
			restModel.setFileName(getFilename());
			restModel.setFileSize(xml.getSize());
			restModel.setChecksum(xml.getChecksum());
			restModel.setXmlSchemaName(xml.getSchemaName());
		}
		
		return restModel;
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		remove();
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		XmlGraphFieldImpl field = getGraph().addFramedEdge(container, getXml(), HAS_FIELD, XmlGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());

		// Clone all properties except the uuid and the type.
		for (String key : getPropertyKeys()) {
			if (key.equals("uuid") || key.equals("ferma_type")) {
				continue;
			}
			Object value = getProperty(key);
			field.setProperty(key, value);
		}
		return field;
	}

	@Override
	public void validate() {
	}

	@Override
	public Xml getXml() {
		return inV().nextOrDefaultExplicit(XmlImpl.class, null);
	}

}
