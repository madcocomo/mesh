package com.gentics.mesh.core.csdb.impl;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;

/**
 * Node populator implementation for S1000D Data Module files (issue neutral).
 */
public class S1000dDataModuleNodePopulator extends S1000dXmlNodePopulator {
	
	public static final Pattern PATTERN_DMC = Pattern.compile("^(?:DMC-|DME-\\w+-\\w+-)((?:\\w+-){7}\\w).*$");
	public static final String TEMPLATE_DMC = "DMC-%s.XML";
	
	@Override
	public int getPriority() {
		return PRIORITY_BASE + 1000;
	}

	@Override
	public boolean accept(Path file, Map<String, Object> context) {
		String filename = file.getFileName().toString();
		return super.accept(file, context) 
			&& PATTERN_DMC.matcher(filename).matches();
	}

	@Override
	public String parseNodeName(Path file) {
		Matcher matcher = PATTERN_DMC.matcher(file.getFileName().toString());
		return String.format(TEMPLATE_DMC, matcher.group(1));
	}
	
	@Override
	public void populateContent(Path file, NodeGraphFieldContainer node) {
		// Set display name.
		populateDisplayName(file, node);
		
		// Get and set the "references" field if possible.
		// TODO
		
		// Get and set the "content" field if possible.
		populateXmlField(file, node, FIELD_NAME_CONTENT);
	}
}
