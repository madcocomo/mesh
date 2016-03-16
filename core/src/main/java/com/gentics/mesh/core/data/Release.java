package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * Interface for Release Vertex
 */
public interface Release extends MeshCoreVertex<ReleaseResponse, Release>, NamedElement, ReferenceableElement<ReleaseReference> {
	public static final String TYPE = "release";

	/**
	 * Get whether the release is active
	 * @return true for active release
	 */
	boolean isActive();

	/**
	 * Set whether the release is active
	 * @param active true for active
	 */
	void setActive(boolean active);

	/**
	 * Get the next Release
	 * @return next Release
	 */
	Release getNextRelease();

	/**
	 * Set the next Release
	 * @param release next Release
	 */
	void setNextRelease(Release release);

	/**
	 * Get the previous Release
	 * @return previous Release
	 */
	Release getPreviousRelease();

	/**
	 * Get the root vertex
	 * @return
	 */
	ReleaseRoot getRoot();

	/**
	 * Assign the given schema version to the release.
	 * Unassign all other schema versions of the schema
	 * @param schemaContainerVersion
	 */
	void assignSchemaVersion(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Unassign all schema versions of the given schema from this release
	 * @param schemaContainer
	 */
	void unassignSchema(SchemaContainer schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this release
	 *
	 * @param schema schema
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainer schema);

	/**
	 * Check whether the given schema container version is assigned to this release
	 *
	 * @param schemaContainerVersion schema container version
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Get the schema container version of the given schema container, that is assigned to this release or null if not assigned at all
	 * @param schemaContainer schema container
	 * @return assigned version or null
	 */
	SchemaContainerVersion getVersion(SchemaContainer schemaContainer);

	/**
	 * Get list of all schema container versions
	 * @return list
	 * @throws InvalidArgumentException
	 */
	List<? extends SchemaContainerVersion> findAllSchemaVersions() throws InvalidArgumentException;
}