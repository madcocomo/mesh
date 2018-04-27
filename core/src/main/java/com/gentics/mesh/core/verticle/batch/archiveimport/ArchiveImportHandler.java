package com.gentics.mesh.core.verticle.batch.archiveimport;

import com.gentics.mesh.core.verticle.handler.AbstractHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ArchiveImportHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ArchiveImportHandler.class);
	
	
	/**
	 * Imports the archive into the starting from the specified node.
	 * 
	 * The target node must be of a container-capable schema (i.e. a folder).
	 * 
	 * Missing target nodes will be created.
	 * 
	 * 
	 */
}
