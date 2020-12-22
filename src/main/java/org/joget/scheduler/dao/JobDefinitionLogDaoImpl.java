package org.joget.scheduler.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.spring.model.AbstractSpringDao;
import org.joget.commons.util.LogUtil;
import org.joget.scheduler.model.JobDefinitionLog;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class JobDefinitionLogDaoImpl extends AbstractSpringDao implements JobDefinitionLogDao {

    public Collection<JobDefinitionLog> find(final String condition, final Object[] params, final String sort, final Boolean desc, final Integer start, final Integer rows) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Collection<JobDefinitionLog> jobDefinitionLogs = (Collection<JobDefinitionLog>) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    return (Collection<JobDefinitionLog>) find("JobDefinitionLog", condition, params, sort, desc, start, rows);
                }
            });
            return jobDefinitionLogs;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionLogDaoImpl.class.getName(), e, "Get Job Definition Logs Error!");
            return null;
        }
    }

    public Collection<JobDefinitionLog> findByJobId(final String jobId, final String condition, final Object[] params, final String sort, final Boolean desc, final Integer start, final Integer rows) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Collection<JobDefinitionLog> jobDefinitionLogs = (Collection<JobDefinitionLog>) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    String conds = condition;
                    if (conds == null || conds.isEmpty()) {
                        conds = "where e.jobId = ?";
                    } else {
                        conds += " and e.jobId = ?";
                    }
                    List list = new ArrayList();
                    if (params != null) {
                        list.addAll(Arrays.asList(params));
                    }
                    list.add(jobId);
                    
                    return (Collection<JobDefinitionLog>) find("JobDefinitionLog", conds, list.toArray(), sort, desc, start, rows);
                }
            });
            return jobDefinitionLogs;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionLogDaoImpl.class.getName(), e, "Get Job Definition Logs By JobId Error!");
            return null;
        }
    }

    public Long count(final String condition, final Object[] params) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Long count = (Long) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    return count("JobDefinitionLog", condition, params);
                }
            });
            return count;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionLogDaoImpl.class.getName(), e, "Count Job Definition Log Error!");
            return null;
        }
    }

    public Long countByJobId(final String jobId, final String condition, final Object[] params) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Long count = (Long) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    String conds = condition;
                    if (conds == null || conds.isEmpty()) {
                        conds = "where e.jobId = ?";
                    } else {
                        conds += " and e.jobId = ?";
                    }
                    List list = new ArrayList();
                    if (params != null) {
                        list.addAll(Arrays.asList(params));
                    }
                    list.add(jobId);
                    return count("JobDefinitionLog", conds, list.toArray());
                }
            });
            return count;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionLogDaoImpl.class.getName(), e, "Count Job Definition Log Error!");
            return null;
        }
    }

    public void save(final JobDefinitionLog jobDefinitionLog) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    saveOrUpdate("JobDefinitionLog", jobDefinitionLog);
                    return true;
                }
            });
        } catch (Exception e) {
            LogUtil.error(JobDefinitionLogDaoImpl.class.getName(), e, "Save Job Definition Log Error!");
        }
    }

    public void deleteByJobId(String jobId) {
        try {
            final Collection<JobDefinitionLog> logs = findByJobId(jobId, null, null, null, null, null, null);
            if (logs != null && !logs.isEmpty()) {
                TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
                transactionTemplate.execute(new TransactionCallback<Object>() {
                    public Object doInTransaction(TransactionStatus ts) {
                        for (JobDefinitionLog l : logs) {
                            delete("JobDefinitionLog", l);
                        }
                        return true;
                    }
                });
            }
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Delete Job Definition Logs Error!");
        }
    }
    
}
