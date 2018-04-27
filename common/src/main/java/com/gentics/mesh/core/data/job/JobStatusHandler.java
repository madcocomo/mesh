package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.job.JobStatus;

/**
 * Callbacks for jobs.
 */
public interface JobStatusHandler {

	/**
	 * Update the status and store it in the local or cluster wide map.
	 * 
	 * @return Fluent API
	 */
	JobStatusHandler commit();

	/**
	 * Update status and inform all the channels.
	 * 
	 * @return Fluent API
	 */

	JobStatusHandler done();

	/**
	 * Handle the error and inform all channels.
	 * 
	 * @param error
	 * @param string
	 * @return Fluent API
	 */
	JobStatusHandler error(Throwable error, String string);

	/**
	 * Set the version edge which will store the migration status.
	 * 
	 * @param versionEdge
	 */
	void setVersionEdge(ReleaseVersionEdge versionEdge);

	/**
	 * Set the current status.
	 * 
	 * @param status
	 */
	void setStatus(JobStatus status);

	/**
	 * Set the current completion count.
	 * 
	 * @param completionCount
	 */
	void setCompletionCount(long completionCount);

	void incCompleted();

}
