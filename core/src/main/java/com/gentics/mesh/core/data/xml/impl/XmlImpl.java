package com.gentics.mesh.core.data.xml.impl;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.xml.Xml;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.storage.BinaryStorage;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * @see Xml.
 */
public class XmlImpl extends MeshVertexImpl implements Xml {
	
	public static void init(Database database) {
		database.addVertexType(XmlImpl.class, MeshVertexImpl.class);
	}

	// TODO Reuse instead of copying from BinaryImpl
	@Override
	public Flowable<Buffer> getStream() {
		BinaryStorage storage = MeshInternal.get().binaryStorage();
		return storage.read(getUuid());
	}
	
	@Override
	public void remove() {
		BinaryStorage storage = MeshInternal.get().binaryStorage();
		storage.delete(getUuid()).blockingAwait();
		super.remove();
	}
}
