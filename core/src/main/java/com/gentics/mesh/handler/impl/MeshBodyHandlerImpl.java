package com.gentics.mesh.handler.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;

/**
 * Wrapper class to avoid stupid default constructor behaviour.
 */
public class MeshBodyHandlerImpl extends BodyHandlerImpl {
	
	private static final Logger log = LoggerFactory.getLogger(MeshBodyHandlerImpl.class);
	
	private final String uploadsDirectory;

	public MeshBodyHandlerImpl(String uploadsDirectory, long uploadLimit) {
		setUploadsDirectory(uploadsDirectory);
		this.setBodyLimit(uploadLimit);
		this.setUploadsDirectory(uploadsDirectory);
		this.setMergeFormAttributes(false);
		this.setDeleteUploadedFilesOnEnd(true);
		this.uploadsDirectory = uploadsDirectory;
	}

	@Override
	public void handle(RoutingContext context) {
		HttpServerRequest request = context.request();
		
		// Use original behavior except for POST/PUT-ing XML and Zip content.
		HttpMethod method = request.method();
		String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
		if (
			!(
				method == HttpMethod.POST
				|| method == HttpMethod.PUT
			) || !(
				contentType.equals("text/xml") 
				|| contentType.equals("application/xml") 
				|| contentType.equals("application/zip")
			)
		) {
			super.handle(context);
			return;
		}
		
		log.info("Using special request body handling for file upload of type {}", contentType);
		
		// !!! UBER DIRTY HACK INBOUND !!!
		// For POST/PUT-ing XML and Zip files, write the request body to the upload folder directly 
		// instead of using the built-in vert.x body handler behavior (i.e. in-memory buffer).
		// This also circumvents the "max upload size" setting.
		Path resultFile = Paths.get(uploadsDirectory, UUID.randomUUID().toString());
		request.handler(buffer -> {
			try {
				// Create if missing.
				if (!Files.exists(resultFile)) {
					Files.createFile(resultFile);
				}
				// Append content of request body buffer to file.
				try (OutputStream os = Files.newOutputStream(resultFile, StandardOpenOption.APPEND)) {
					for (int i = 0, len = buffer.length(); i < len; i++) {
						os.write(buffer.getByte(i));
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		request.endHandler(v -> {
			// Store the file path in the routing context for route handlers to read.
			context.put("__UPLOADED_BODY__", resultFile.toString());
			context.next();
		});
		
		// Signal other body handlers (if any) to stop handling this request.
		context.put("__body-handled", Boolean.TRUE);
	}
}
