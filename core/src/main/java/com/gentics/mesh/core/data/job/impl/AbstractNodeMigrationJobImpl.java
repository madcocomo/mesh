package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.job.JobResponse;

public abstract class AbstractNodeMigrationJobImpl extends JobImpl {

	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = super.transformToRestSync(ac, level, languageTags);
		Map<String, String> props = response.getProperties();
		if (getFromSchemaVersion() != null && getToSchemaVersion() != null) {
			SchemaContainer container = getToSchemaVersion().getSchemaContainer();
			props.put("schemaName", container.getName());
			props.put("schemaUuid", container.getUuid());
			props.put("fromVersion", getFromSchemaVersion().getVersion());
			props.put("toVersion", getToSchemaVersion().getVersion());
		}
		return response;
	}

	public SchemaContainerVersion getFromSchemaVersion() {
		return out(HAS_FROM_VERSION)
			.has(SchemaContainerVersionImpl.class)
			.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	public void setFromSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_FROM_VERSION);
	}

	public SchemaContainerVersion getToSchemaVersion() {
		return out(HAS_TO_VERSION)
			.has(SchemaContainerVersionImpl.class)
			.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	public void setToSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_TO_VERSION);
	}
}
