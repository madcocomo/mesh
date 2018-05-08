package com.gentics.mesh.core.csdb;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.error.Errors;

public class NodePopulatorService {

	// Singleton instance.
	private static NodePopulatorService instance;
	
	/**
	 * Gets the lazy-initialized singleton instance.
	 * 
	 * @return The instance.
	 */
	public static synchronized NodePopulatorService getInstance() {
		if (instance == null) {
			instance = new NodePopulatorService();
		}
		return instance;
	}
	
	/** List of populators. */
	private List<NodePopulator> populatorList;
	
	/** Hidden constructor. */
	private NodePopulatorService() {}
	
	/**
	 * Populates the content of the specified node using the source file.
	 * 
	 * Same effect as calling getNodePopulatorFailfast(file).populateContent(file, node);
	 * 
	 * @param file The source file.
	 * @param node The target node.
	 */
	public void populateNode(Path file, NodeGraphFieldContainer node) {
		getNodePopulatorFailfast(file).populateContent(file, node);
	}
	
	/**
	 * Selects a suitable node populator for the file.
	 * 
	 * @param file The file to test.
	 * @return The populator found, or null.
	 */
	public NodePopulator getNodePopulator(Path file) {
		Map<String, Object> context = new HashMap<>();
		for (NodePopulator nodePopulator : getPopulators()) {
			if (nodePopulator.accept(file, context)) {
				return nodePopulator;
			}
		}
		return null;
	}
	
	/**
	 * Like getNodePopulator but fail-fast, i.e. throws exception instead of returning null.
	 * @param file The file to test.
	 * @return The populator found.
	 */
	public NodePopulator getNodePopulatorFailfast(Path file) {
		NodePopulator np = getNodePopulator(file);
		if (np == null) {
			throw Errors.internalError("csdb_error_finding_suitable_populator", file.toString());
		}
		return np;
	}
	
	// Gets the list of populators sorted by priority from highest to lowerest.
	public synchronized final List<NodePopulator> getPopulators() {
		if (populatorList == null) {
			List<NodePopulator> tempList = new ArrayList<>();
			// Lazy-load SPI implementations.
			ServiceLoader<NodePopulator> serviceLoader = ServiceLoader.load(NodePopulator.class);
			for (Iterator<NodePopulator> iter = serviceLoader.iterator(); iter.hasNext();) {
				tempList.add(iter.next());
			}
			Collections.sort(tempList, (NodePopulator np1, NodePopulator np2) -> {
				return np1.getPriority() - np2.getPriority();
			});
			this.populatorList = Collections.unmodifiableList(tempList);
		}
		return populatorList;
	}
}
