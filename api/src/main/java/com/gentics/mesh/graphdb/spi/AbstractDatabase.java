package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.Trx;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected StorageOptions options;
	protected FramedThreadedTransactionalGraph fg;

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph() {
		return fg;
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		try (Trx tx = new Trx(this)) {
			getFramedGraph().e().removeAll();
			getFramedGraph().v().removeAll();
			tx.success();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

	@Override
	public void init(StorageOptions options) {
		this.options = options;
		start();
	}

	@Override
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting graph database");
		}
		stop();
		try {
			if (options.getDirectory() != null) {
				FileUtils.deleteDirectory(new File(options.getDirectory()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

}