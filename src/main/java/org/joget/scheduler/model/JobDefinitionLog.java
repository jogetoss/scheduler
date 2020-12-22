package org.joget.scheduler.model;

import java.util.Date;

public class JobDefinitionLog {
    private String id;
    private String jobId;
    private String message;
    private Date startTime;
    private Date endTime;
    private Long timeTakenInMS;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Long getTimeTakenInMS() {
        return timeTakenInMS;
    }

    public void setTimeTakenInMS(Long timeTakenInMS) {
        this.timeTakenInMS = timeTakenInMS;
    }
}
