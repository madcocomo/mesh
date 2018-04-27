package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobStatusHandler;
import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The job status class keeps track of the status of a job run and manages also the errors and event handling.
 */
public class JobStatusHandlerImpl implements JobStatusHandler {

	private static final Logger log = LoggerFactory.getLogger(JobStatusHandlerImpl.class);

	private Vertx vertx;

	private ReleaseVersionEdge versionEdge;

	private Job job;

	private long completionCount = 0;

	private JobStatus status;

	public JobStatusHandlerImpl(Job job, Vertx vertx) {
		this.vertx = vertx;
		this.job = job;
		status = job.getStatus();
	}

	@Override
	public JobStatusHandler commit() {
		if (versionEdge != null) {
			versionEdge.setMigrationStatus(status);
		}
		job.setCompletionCount(completionCount);
		job.setStatus(status);

		Tx.getActive().getGraph().commit();
		return this;
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the success</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * <li>Update the job and potential version edge</li>
	 * </ul>
	 */
	public JobStatusHandler done() {
		setStatus(JobStatus.COMPLETED);
		log.info("Migration job of type {} completed without errors.", job.getType());
		JsonObject result = new JsonObject().put("type", "completed");
		vertx.eventBus().publish(MESH_MIGRATION, result);
		job.setStopTimestamp();
		commit();
		return this;
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the error</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param error
	 * @param failureMessage
	 */
	public JobStatusHandler error(Throwable error, String failureMessage) {
		setStatus(JobStatus.FAILED);
		log.error("Error handling migration", error);

		vertx.eventBus().publish(MESH_MIGRATION, new JsonObject().put("type", status.name()));
		job.setStopTimestamp();
		job.setError(error);
		commit();
		return this;
	}

	@Override
	public void setVersionEdge(ReleaseVersionEdge versionEdge) {
		this.versionEdge = versionEdge;
	}

	@Override
	public void setCompletionCount(long completionCount) {
		this.completionCount = completionCount;
	}

	@Override
	public void setStatus(JobStatus status) {
		this.status = status;
	}

	@Override
	public void incCompleted() {
		completionCount++;
	}

}
