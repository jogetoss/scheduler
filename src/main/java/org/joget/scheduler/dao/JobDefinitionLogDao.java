package org.joget.scheduler.dao;

import java.util.Collection;
import org.joget.scheduler.model.JobDefinitionLog;

public interface JobDefinitionLogDao {
    public Collection<JobDefinitionLog> find(String condition, Object[] params, String sort, Boolean desc, Integer start, Integer rows);
    
    public Collection<JobDefinitionLog> findByJobId(String jobId, String condition, Object[] params, String sort, Boolean desc, Integer start, Integer rows);
    
    public Long count(String condition, Object[] params);
    
    public Long countByJobId(String jobId, String condition, Object[] params);
    
    public void save(JobDefinitionLog jobDefinitionLog);
    
    public void deleteByJobId(String jobId);
}
