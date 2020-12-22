package org.joget.scheduler.dao;

import java.util.Collection;
import org.joget.scheduler.model.JobDefinition;

public interface JobDefinitionDao {
    public Collection<JobDefinition> find(String condition, Object[] params, String sort, Boolean desc, Integer start, Integer rows);
    
    public Long count(String condition, Object[] params);
    
    public boolean hasPendingJob();
    
    public JobDefinition get(String id);
    
    public void save(JobDefinition jobDefinition);
    
    public void delete(String id);
}
