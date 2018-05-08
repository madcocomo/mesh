package com.gentics.mesh.core.verticle.batch.archiveimport;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.csdb.NodePopulator;
import com.gentics.mesh.core.csdb.NodePopulatorService;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.job.JobStatusHandler;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

import io.netty.handler.codec.http.HttpResponseStatus;

@Singleton
public class ArchiveImportHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ArchiveImportHandler.class);
	
	private Database database;

	private SearchQueue searchQueue;

	@Inject
	public ArchiveImportHandler(Database database, SearchQueue searchQueue) {
		this.database = database;
		this.searchQueue = searchQueue;
	}
	
	/**
	 * Imports the archive into the starting from the specified node.
	 * 
	 * The target root node must be of a container-capable schema (i.e. a folder).
	 * 
	 * Missing target nodes under the root node will be created.
	 * 
	 * @param targetNode The root folder to place the new nodes under.
	 * @param archivePath Path to the archive file.
	 * @param schemaSelector For selecting the appropriate schema for each file.
	 * @param status Job status handler.
	 */
	public void importArchive(
			final User user, final Language language, final Release release, 
			final Node targetNode, final String archivePath, 
			final Function<Path, SchemaContainer> schemaSelector, 
			final JobStatusHandler status) {
		final Project project = release.getProject();
		
		status.setStatus(JobStatus.RUNNING);
		status.commit();
		
		// Extract the archive to a temporary folder.
		log.info("Extracting archive file {}", archivePath);
		MutableInt fileCounter = new MutableInt(0);
		Path archiveDir = extractArchive(archivePath, (Path zipEntry, Path target) -> {
			try {
				Files.copy(zipEntry, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw Errors.error(HttpResponseStatus.INTERNAL_SERVER_ERROR, e, "job_import_error_extracting_file", zipEntry.toString());
			}
			// Report progress every 50 files.
			status.incCompleted();
			fileCounter.increment();
			if (fileCounter.intValue() % 50 == 0) {
				log.info("Extracted {} files", fileCounter.intValue());
				status.commit();
			}
		});
		log.info("Extracted {} files", fileCounter.intValue());
		
		// First pass: Create or find a new / existing node for each file and folder under the root.
		log.info("Finding/Creating nodes");
		MutableInt nodeCounter = new MutableInt(0);
		
		// The result is stored as a map from the source file to its suitable node populator, found or created node
		// and target node version supplier function.
		final Map<Path, Triple<NodePopulator, Node, NodeGraphFieldContainer>> result = new TreeMap<>();
		final NodePopulatorService nodePopulatorService = MeshInternal.get().nodePopulatorService();
		createNodeTree(targetNode, archiveDir, (Node parentNode, Path path) -> {
			// Detect the schema to use for the node.
			SchemaContainer schemaContainer = schemaSelector.apply(path);
			SchemaContainerVersion schemaContainerVersion = schemaContainer.getLatestVersion();
			
			// Find the node populator for the file.
			NodePopulator nodePopulator = nodePopulatorService.getNodePopulatorFailfast(path);
			
			// Determine the display name of the new node.
			String nodeName = nodePopulator.parseNodeName(path);
			
			// Try to find an existing node.
			Node node = findChildNodeByDisplayName(parentNode, nodeName, language, release);
			if (node == null || !Objects.equals(node.getSchemaContainer().getUuid(), schemaContainer.getUuid())) {
				log.info("Missing or incompatible node found for file {}, will create new", path.getFileName());
				node = parentNode.create(user, schemaContainerVersion, project);
			} else {
				log.info("Reuse existing node for file {}", path.getFileName());
			}
			
			// Create a new version based on the existing version (if any).
			NodeGraphFieldContainer oldNodeVersion = node.getGraphFieldContainer(language, release, ContainerType.DRAFT);
			NodeGraphFieldContainer newNodeVersion = node.createGraphFieldContainer(language, release, user, oldNodeVersion, true);
			// Assign a name.
			nodePopulator.populateDisplayName(path, newNodeVersion);
			
			result.put(path, Triple.of(nodePopulator, node, newNodeVersion));
			
			// Report progress if needed.
			status.incCompleted();
			nodeCounter.increment();
			if (nodeCounter.intValue() % 50 == 0) {
				log.info("Created {} nodes", nodeCounter.intValue());
				status.commit();
			}
			
			return node;
		});
		log.info("Finished creating {} nodes", nodeCounter.intValue());
		
		// Second pass: Populate node with content using the NodePopulatorService.
		log.info("Populating nodes");
		MutableInt nodePopCounter = new MutableInt(0);
		for (Map.Entry<Path, Triple<NodePopulator, Node, NodeGraphFieldContainer>> mapEntry : result.entrySet()) {
			Path p = mapEntry.getKey();
			NodePopulator np = mapEntry.getValue().getLeft();
			NodeGraphFieldContainer nodeVersion = mapEntry.getValue().getRight();
			
			// Use the populator to populate the node version's content.
			try {
				np.populateContent(p, nodeVersion);
			} catch (Exception e) {
				throw Errors.internalError(e, "csdb_error_populating_node_with_populator", 
					nodeVersion.getUuid(), p.toString(), np.getClass().getName());
			}
			
			// Report progress if needed.
			status.incCompleted();
			nodePopCounter.increment();
			if (nodePopCounter.intValue() % 50 == 0) {
				log.info("Populated {} nodes", nodeCounter.intValue());
				status.commit();
			}
		}
		log.info("Finished populating {} nodes", nodeCounter.intValue());
		
		// Delete the temp archive dir.
		FileUtils.deleteQuietly(archiveDir.toFile());
		
		log.info("Importing of {} completed", archivePath);
	}
	
	// Extracts the archive to a temporary folder, return the folder.
	private Path extractArchive(final String archivePath, BiConsumer<Path, Path> extractor) {
		Path archive = Paths.get(archivePath);
		if (!Files.exists(archive)) {
			throw Errors.error(HttpResponseStatus.NOT_FOUND, "job_import_missing_archive", archivePath);
		}
		try {
			final Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
			try (FileSystem zipFs = FileSystems.newFileSystem(archive, getClass().getClassLoader())) {
				extractFolderEntry(zipFs.getPath("/"), tempDir, extractor);
			}
			return tempDir;
		} catch (IOException e) {
			throw Errors.error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "job_import_error_extracting_archive", e);
		}
	}
	
	private void extractFolderEntry(Path zipEntry, Path targetRootFolder, BiConsumer<Path, Path> extractor) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(zipEntry)) {
			for (Path subZipEntry : ds) {
				extractEntry(subZipEntry, targetRootFolder, extractor);
			}
		}
	}
	
	private void extractEntry(Path zipEntry, Path targetRootFolder, BiConsumer<Path, Path> extractor) throws IOException {
		Path target = targetRootFolder.resolve(zipEntry.toString().substring(1));
		if (Files.isDirectory(zipEntry)) {
			Files.createDirectories(target);
			extractFolderEntry(zipEntry, targetRootFolder, extractor);
		} else {
			extractor.accept(zipEntry, target);
		}
	}
	
	// Creates a node tree that mirrors the folder structure of the root folder.
	private void createNodeTree(Node rootNode, Path rootFolder, BiFunction<Node, Path, Node> nodeCreator) {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(rootFolder)) {
			for (Path path : ds) {
				createNode(rootNode, path, nodeCreator);
			}
		} catch (Exception e) {
			throw Errors.error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "job_import_error_create_nodes", e);
		}
	}
	
	private void createNodes(Node parentNode, Path folder, BiFunction<Node, Path, Node> nodeCreator) throws IOException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
			for (Path path : ds) {
				createNode(parentNode, path, nodeCreator);
			}
		}
	}
	
	private void createNode(Node parentNode, Path path, BiFunction<Node, Path, Node> nodeCreator) throws IOException {
		Node node = nodeCreator.apply(parentNode, path);
		if (Files.isDirectory(path)) {
			createNodes(node, path, nodeCreator);
		}
	}
	
	// Finds the child node of the parent node whose specified language+release+state version has the specified display name.
	private Node findChildNodeByDisplayName(Node parentNode, String displayName, Language lanugage, Release release) {
		for (Node childNode : parentNode.getChildren(release.getUuid())) {
			NodeGraphFieldContainer nodeVersion = childNode.getGraphFieldContainer(lanugage, release, ContainerType.DRAFT);
			if (nodeVersion != null && nodeVersion.getDisplayFieldValue().equalsIgnoreCase(displayName)) {
				return childNode;
			}
		}
		return null;
	}
}
