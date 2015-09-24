package com.gentics.mesh.graphdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private OrientGraphFactory factory;
	private DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");

	@Override
	public void stop() {
		factory.close();
		Orient.instance().shutdown();
		Database.setThreadLocalGraph(null);
	}

	@Override
	public void clear() {
		factory.getNoTx().getVertices().forEach(v -> {
			v.remove();
		});
	}

	@Override
	public void start() {
		Orient.instance().startup();
		if (options == null || options.getDirectory() == null) {
			log.info("No graph database settings found. Fallback to in memory mode.");
			factory = new OrientGraphFactory("memory:tinkerpop");
		} else {
			// factory = new OrientGraphFactory("plocal:" + options.getDirectory());// .setupPool(5, 100);
			factory = new OrientGraphFactory("plocal:" + options.getDirectory());
		}
		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

	}

	@Override
	public void reload(MeshElement element) {
		((OrientVertex) ((WrappedVertex) element.getElement()).getBaseElement()).reload();
		//((OrientVertex) element.getElement()).reload();
	}

	@Override
	public FramedTransactionalGraph startTransaction() {
		return new DelegatingFramedTransactionalGraph<>(factory.getTx(), true, false);
	}

	@Override
	public Trx trx() {
		return new OrientDBTrx(this);
	}

	@Override
	public FramedGraph startNoTransaction() {
		return new DelegatingFramedGraph<>(factory.getNoTx(), true, false);
	}

	@Override
	public NoTrx noTrx() {
		return new OrientDBNoTrx(this);
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			String dateString = formatter.format(new Date());
			String backupFile = "backup_" + dateString + ".zip";
			OutputStream out = new FileOutputStream(new File(backupDirectory, backupFile).getAbsolutePath());
			db.backup(out, null, null, listener, 9, 2048);
		} finally {
			db.close();
		}
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			InputStream in = new FileInputStream(backupFile);
			db.restore(in, null, null, listener);
		} finally {
			db.close();
		}

	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};

			String dateString = formatter.format(new Date());
			String exportFile = "export_" + dateString;

			ODatabaseExport export = new ODatabaseExport(db, new File(outputDirectory, exportFile).getAbsolutePath(), listener);
			export.exportDatabase();
			export.close();
		} finally {
			db.close();
		}
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			ODatabaseImport databaseImport = new ODatabaseImport(db, importFile, listener);
			databaseImport.importDatabase();
			databaseImport.close();
		} finally {
			db.close();
		}

	}

}
