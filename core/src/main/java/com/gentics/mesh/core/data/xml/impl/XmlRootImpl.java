package com.gentics.mesh.core.data.xml.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_XML;

import java.util.Iterator;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.core.data.xml.XmlRoot;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class XmlRootImpl extends MeshVertexImpl implements XmlRoot {
	
	public static void init(Database database) {
		database.addVertexType(XmlRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_XML, true, false, true);
	}

	@Override
	public Database database() {
		return MeshInternal.get().database();
	}

	@Override
	public Class<? extends Xml> getPersistanceClass() {
		return XmlImpl.class;
	}
	
	public Xml findByUuid(String uuid) {
		FramedGraph graph = Tx.getActive().getGraph();
		// 1. Find the element with given uuid within the whole graph
		Iterator<Vertex> it = database().getVertices(getPersistanceClass(), new String[] { MeshVertex.UUID_KEY }, new String[] { uuid });
		if (it.hasNext()) {
			Vertex potentialElement = it.next();
			// 2. Use the edge index to determine whether the element is part of this root vertex
			String key = "e." + HAS_XML.toLowerCase() + "_inout";
			Object value = database().createComposedIndexKey(potentialElement.getId(), getId());
			Iterable<Edge> edges = graph.getEdges(key, value);
			if (edges.iterator().hasNext()) {
				return graph.frameElementExplicit(potentialElement, getPersistanceClass());
			}
		}
		return null;
	}
	
	@Override
	public Xml createItem() {
		Xml xml = getGraph().addFramedVertex(XmlImpl.class);
		addItem(xml);
		return xml;
	}
	
}
