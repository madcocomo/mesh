package com.gentics.mesh.core.data.xml;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_XML;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;

public interface XmlRoot extends MeshVertex {
	
	Database database();
	
	Class<? extends Xml> getPersistanceClass();
	
	Xml findByUuid(String uuid);
	
	Xml createItem();
	
	default public void addItem(Xml item) {
		FramedGraph graph = getGraph();
		final String key = "e." + HAS_XML.toLowerCase() + "_inout";
		final Object value = database().createComposedIndexKey(item.getId(), getId());
		Iterable<Edge> edges = graph.getEdges(key, value);
		if (!edges.iterator().hasNext()) {
			linkOut(item, HAS_XML);
		}
	}

	default public void removeItem(Xml item) {
		unlinkOut(item, HAS_XML);
	}
}
