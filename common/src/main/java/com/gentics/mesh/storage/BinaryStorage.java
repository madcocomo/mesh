package com.gentics.mesh.storage;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * A binary storage provides means to store and retrieve binary data.
 */
public interface BinaryStorage {

	/**
	 * Stores the contents of the stream.
	 * 
	 * @param stream
	 * @param uuid
	 *            Uuid of the binary to be stored
	 * @return
	 */
	Completable store(Flowable<Buffer> stream, String uuid);

	/**
	 * Checks whether the data by that UUID exists
	 * 
	 * @param uuid UUID of the binary
	 *
	 * @return
	 */
	boolean exists(String uuid);

	/**
	 * Read the binary data which is identified by the given binary uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Flowable<Buffer> read(String uuid);

	/**
	 * Delete the binary with the given uuid.
	 * 
	 * @param uuid
	 */
	Completable delete(String uuid);

}
