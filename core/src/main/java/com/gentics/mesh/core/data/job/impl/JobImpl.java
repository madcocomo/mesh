package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.util.ETag;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Job
 */
public abstract class JobImpl extends AbstractMeshCoreVertex<JobResponse, Job> implements Job {

	private static final Logger log = LoggerFactory.getLogger(JobImpl.class);

	private static final String ERROR_DETAIL_MAX_LENGTH_MSG = "..." + System.lineSeparator() +
		"For further details concerning this error please refer to the logs.";

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		throw new NotImplementedException("Jobs can't be updated");
	}

	@Override
	public TypeInfo getTypeInfo() {
		return null;
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = new JobResponse();
		response.setUuid(getUuid());

		User creator = getCreator();
		if (creator != null) {
			response.setCreator(creator.transformToReference());
		} else {
			log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no creator. Omitting creator field");
		}

		String date = getCreationDate();
		response.setCreated(date);
		response.setErrorMessage(getErrorMessage());
		response.setErrorDetail(getErrorDetail());
		response.setType(getType());
		response.setStatus(getStatus());
		response.setStopDate(getStopDate());
		response.setStartDate(getStartDate());
		response.setCompletionCount(getCompletionCount());
		response.setNodeName(getNodeName());

		Map<String, String> props = response.getProperties();
		props.put("releaseName", getRelease().getName());
		props.put("releaseUuid", getRelease().getUuid());

		return response;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid() + getErrorMessage() + getErrorDetail());
	}

	@Override
	public void setType(String type) {
		setProperty(TYPE_PROPERTY_KEY, type);
	}

	@Override
	public String getType() {
		return getProperty(TYPE_PROPERTY_KEY);
	}

	@Override
	public Long getStartTimestamp() {
		return getProperty(START_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStartTimestamp(Long date) {
		setProperty(START_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public Long getStopTimestamp() {
		return getProperty(STOP_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStopTimestamp(Long date) {
		setProperty(STOP_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public long getCompletionCount() {
		Long value = getProperty(COMPLETION_COUNT_PROPERTY_KEY);
		return value == null ? 0 : value;
	}

	@Override
	public void setCompletionCount(long count) {
		setProperty(COMPLETION_COUNT_PROPERTY_KEY, count);
	}

	@Override
	public Release getRelease() {
		return out(HAS_RELEASE).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public void setRelease(Release release) {
		setUniqueLinkOutTo(release, HAS_RELEASE);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		remove();
	}

	@Override
	public JobStatus getStatus() {
		String status = getProperty(STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return JobStatus.valueOf(status);
	}

	@Override
	public void setStatus(JobStatus status) {
		setProperty(STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public String getErrorDetail() {
		return getProperty(ERROR_DETAIL_PROPERTY_KEY);
	}

	@Override
	public void setErrorDetail(String info) {
		// truncate the error detail message to the max length for the error detail property
		if (info != null && info.length() > ERROR_DETAIL_MAX_LENGTH) {
			info = info.substring(0, ERROR_DETAIL_MAX_LENGTH - ERROR_DETAIL_MAX_LENGTH_MSG.length()) + ERROR_DETAIL_MAX_LENGTH_MSG;
		}
		setProperty(ERROR_DETAIL_PROPERTY_KEY, info);
	}

	@Override
	public String getErrorMessage() {
		return getProperty(ERROR_MSG_PROPERTY_KEY);
	}

	@Override
	public void setErrorMessage(String message) {
		setProperty(ERROR_MSG_PROPERTY_KEY, message);
	}

	@Override
	public void setError(Throwable e) {
		setErrorDetail(ExceptionUtils.getStackTrace(e));
		setErrorMessage(e.getMessage());
	}

	@Override
	public boolean hasFailed() {
		return getErrorMessage() != null || getErrorDetail() != null;
	}

	@Override
	public void markAsFailed(Exception e) {
		setError(e);
	}

	@Override
	public void resetJob() {
		setStartTimestamp(null);
		setStopTimestamp(null);
		setErrorDetail(null);
		setErrorMessage(null);
		setStatus(JobStatus.QUEUED);
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void process() {
		log.info("Processing job {} of type {}", getUuid(), getType());
		DB.get().tx(() -> {
			setStartTimestamp();
			setStatus(STARTING);
			setNodeName();
		});

		processTask();
	}

	/**
	 * Actual implementation of the task which the job executes.
	 */
	protected abstract void processTask();

	@Override
	public String getNodeName() {
		return getProperty(NODE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNodeName(String nodeName) {
		setProperty(NODE_NAME_PROPERTY_KEY, nodeName);
	}

}
