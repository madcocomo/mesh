package com.gentics.mesh.core.data.release.impl;

import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.job.JobStatus;

/**
 * Abstract implementation for {@link ReleaseMicroschemaEdgeImpl} and {@link ReleaseSchemaEdgeImpl}.
 */
public abstract class AbstractVersionEdge extends MeshEdgeImpl implements ReleaseVersionEdge {

	@Override
	public void setMigrationStatus(JobStatus status) {
		setProperty(MIGRATION_STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public JobStatus getMigrationStatus() {
		String status = getProperty(MIGRATION_STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return JobStatus.valueOf(status);
	}

	@Override
	public Release getRelease() {
		return outV().nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public String getJobUuid() {
		return getProperty(JOB_UUID_PROPERTY_KEY);
	}

	@Override
	public void setJobUuid(String uuid) {
		setProperty(JOB_UUID_PROPERTY_KEY, uuid);
	}

	@Override
	public boolean isActive() {
		return getProperty(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public void setActive(boolean active) {
		setProperty(ACTIVE_PROPERTY_KEY, active);
	}

}
