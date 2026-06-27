package org.joget.scheduler;

import jakarta.servlet.http.HttpServletRequest;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.model.JobDefinition;
import org.joget.workflow.util.WorkflowUtil;

public class SchedulerToggleAction extends DataListActionDefault {

    public String getName() {
        return "SchedulerToggleAction";
    }

    public String getVersion() {
        return "8.0.0";
    }

    public String getDescription() {
        return "";
    }

    public String getLinkLabel() {
        return getPropertyString("label");
    }

    public String getHref() {
        return getPropertyString("href");
    }

    public String getTarget() {
        return "post";
    }

    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    public String getConfirmation() {
        return getPropertyString("confirmation");
    }

    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");

        
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
            return result;
        }

        if (rowKeys != null && rowKeys.length > 0) {
            boolean enable = "enable".equals(getPropertyString("mode"));
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            for (String id : rowKeys) {
                JobDefinition job = jobDefinitionDao.get(id);
                if (job != null) {
                    if (enable) {
                        job.setEnabled(true);
                        SchedulerUtil.resumeJob(job);
                    } else {
                        job.setEnabled(false);
                        SchedulerUtil.pauseJob(job);
                    }
                    jobDefinitionDao.save(job);
                }
            }
        }

        return result;
    }

    public String getLabel() {
        return "SchedulerToggleAction";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return "";
    }
}
