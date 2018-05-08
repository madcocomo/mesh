package com.gentics.mesh.core.csdb.impl;

import java.nio.file.Path;
import java.util.Map;

import com.gentics.mesh.core.csdb.NodePopulator;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;

/**
 * The default node populator that has the absolute lowerest priority and accepts every type of node.
 * 
 * All it does is setting the display name to that of the file name and nothing else.
 */
public class DefaultNodePopulator implements NodePopulator {
	
	@Override
	public int getPriority() {
		return PRIORITY_BASE - 1000;
	}

	@Override
	public boolean accept(Path file, Map<String, Object> context) {
		return true;
	}
	
	public String parseNodeName(Path file) {
		return file.getFileName().toString();
	}

	@Override
	public void populateContent(Path file, NodeGraphFieldContainer node) {
		populateDisplayName(file, node);
	}
}
