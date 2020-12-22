package org.joget.scheduler;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.model.JobDefinition;

public class SchedulerFormBinder extends FormBinder implements FormLoadBinder, FormStoreBinder {

    @Override
    public String getName() {
        return "SchedulerFormBinder";
    }

    @Override
    public String getVersion() {
        return "6.0.0";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return "SchedulerFormBinder";
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet results = new FormRowSet();
        if (primaryKey != null && primaryKey.trim().length() > 0) {
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            JobDefinition jobDefinition = jobDefinitionDao.get(primaryKey);
            
            if (jobDefinition != null) {
                FormRow row = new FormRow();
                row.setId(jobDefinition.getId());
                row.setProperty("applicationId", jobDefinition.getAppId());
                row.setProperty("name", jobDefinition.getName());
                row.setProperty("pluginClass", jobDefinition.getPluginClass());
                row.setProperty("pluginProperties", PropertyUtil.propertiesJsonLoadProcessing(jobDefinition.getPluginProperties()));
                row.setProperty("trigger", jobDefinition.getTrigger());
                
                results.add(row);
            }
        }
        return results;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        if (rows != null && !rows.isEmpty()) {
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            FormRow row = rows.get(0);
            JobDefinition jobDefinition = null; 
            
            if (row.getProperty("pluginProperties") == null || row.getProperty("pluginProperties").isEmpty()) {
                formData.addFormError("pluginClass", AppPluginUtil.getMessage("userview.scheduler.pleaseConfigure", SchedulerMenu.class.getName(), SchedulerMenu.MESSAGE_PATH));
                return rows;
            }
            
            if (row.getId() != null) {
                jobDefinition = jobDefinitionDao.get(row.getId());
            }
            if (jobDefinition == null) {
                jobDefinition = new JobDefinition();
                jobDefinition.setId(UuidGenerator.getInstance().getUuid());
            }
            
            jobDefinition.setAppId(row.getProperty("applicationId"));
            jobDefinition.setName(row.getProperty("name"));
            jobDefinition.setPluginClass(row.getProperty("pluginClass"));
            jobDefinition.setPluginProperties(PropertyUtil.propertiesJsonStoreProcessing(jobDefinition.getPluginProperties(), row.getProperty("pluginProperties")));
            jobDefinition.setTrigger(row.getProperty("trigger"));
            
            SchedulerUtil.scheduleJob(jobDefinition);
            if (jobDefinition.getNextFireTime() != null) {
                jobDefinitionDao.save(jobDefinition);
            } else {
                formData.addFormError("trigger", AppPluginUtil.getMessage("userview.scheduler.invalidSyntax", SchedulerMenu.class.getName(), SchedulerMenu.MESSAGE_PATH));
            }
        }
        return rows;
    }
}