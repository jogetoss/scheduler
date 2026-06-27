package org.joget.scheduler;

import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.ApplicationPlugin;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.dao.JobDefinitionLogDao;
import org.joget.scheduler.model.JobDefinition;
import org.joget.scheduler.model.JobDefinitionLog;
import org.joget.workflow.util.WorkflowUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SchedulerJob implements Job {
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        String jobId = jec.getJobDetail().getKey().getName();
        LogUtil.debug(SchedulerJob.class.getName(), "Executing Job... " + jobId);
        JobDefinitionLog log = new JobDefinitionLog();
        log.setId(UuidGenerator.getInstance().getUuid());
        log.setJobId(jobId);
        log.setStartTime(new Date());
        
        JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
        JobDefinitionLogDao jobDefinitionLogDao = (JobDefinitionLogDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionLogDao");
        JobDefinition jobDefinition = jobDefinitionDao.get(jobId);
        AppDefinition orgAppDef = AppUtil.getCurrentAppDefinition();
        try {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            AppDefinition appDef = appService.getPublishedAppDefinition(jobDefinition.getAppId());
            Plugin plugin = pluginManager.getPlugin(jobDefinition.getPluginClass());
            if (plugin != null) {
                Map propertiesMap = AppPluginUtil.getDefaultProperties(plugin, jobDefinition.getPluginProperties(), appDef, null);
                propertiesMap.put("pluginManager", pluginManager);
                propertiesMap.put("appDef", appDef);

                // add HttpServletRequest into the property map
                try {
                    HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                    if (request != null) {
                        propertiesMap.put("request", request);
                    }
                } catch (Exception e) {
                    // ignore if class is not found
                }

                ApplicationPlugin appPlugin = (ApplicationPlugin) plugin;

                if (appPlugin instanceof PropertyEditable) {
                    ((PropertyEditable) appPlugin).setProperties(propertiesMap);
                }
                appPlugin.execute(propertiesMap);
            }
            
        } catch (Exception e) {
            LogUtil.error(SchedulerJob.class.getName(), e, jobId);
            log.setMessage(e.getLocalizedMessage());
        } finally {
            AppUtil.setCurrentAppDefinition(orgAppDef);
            jobDefinition.setNextFireTime(jec.getNextFireTime());
            jobDefinitionDao.save(jobDefinition);
            log.setEndTime(new Date());
            log.setTimeTakenInMS((log.getEndTime().getTime()-log.getStartTime().getTime()));
            jobDefinitionLogDao.save(log);
        }
    }
}
