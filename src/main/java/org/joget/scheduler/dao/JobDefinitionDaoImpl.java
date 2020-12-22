package org.joget.scheduler.dao;

import java.util.Collection;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.spring.model.AbstractSpringDao;
import org.joget.commons.util.LogUtil;
import org.joget.scheduler.model.JobDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class JobDefinitionDaoImpl extends AbstractSpringDao implements JobDefinitionDao {

    public Collection<JobDefinition> find(final String condition, final Object[] params, final String sort, final Boolean desc, final Integer start, final Integer rows) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Collection<JobDefinition> jobDefinitions = (Collection<JobDefinition>) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    return (Collection<JobDefinition>) find("JobDefinition", condition, params, sort, desc, start, rows);
                }
            });
            return jobDefinitions;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Get Job Definitions Error!");
            return null;
        }
    }

    public Long count(final String condition, final Object[] params) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Long count = (Long) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    return count("JobDefinition", condition, params);
                }
            });
            return count;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Count Job Definitions Error!");
            return null;
        }
    }

    public JobDefinition get(final String id) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            JobDefinition jobDefinition = (JobDefinition) transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    return (JobDefinition) find("JobDefinition", id);
                }
            });
            return jobDefinition;
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Get Job Definition Error!");
            return null;
        }
    }

    public void save(final JobDefinition jobDefinition) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            transactionTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    saveOrUpdate("JobDefinition", jobDefinition);
                    return true;
                }
            });
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Save Job Definition Error!");
        }
    }

    public void delete(String id) {
        try {
            final JobDefinition jobDefinition = get(id);
            if (jobDefinition != null) {
                TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
                transactionTemplate.execute(new TransactionCallback<Object>() {
                    public Object doInTransaction(TransactionStatus ts) {
                        delete("JobDefinition", jobDefinition);
                        return true;
                    }
                });
            }
        } catch (Exception e) {
            LogUtil.error(JobDefinitionDaoImpl.class.getName(), e, "Delete Job Definition Error!");
        }
    }

    public boolean hasPendingJob() {
        Long count = count("where e.nextFireTime is not null", null);
        return count > 0;
    }
}
