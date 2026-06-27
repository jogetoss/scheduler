package org.joget.scheduler;

import javax.servlet.http.HttpServletRequest;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.dao.JobDefinitionLogDao;
import org.joget.scheduler.model.JobDefinition;
import org.joget.workflow.util.WorkflowUtil;

public class SchedulerDeleteAction extends DataListActionDefault {

    public String getName() {
        return "SchedulerDeleteAction";
    }

    public String getVersion() {
        return "6.0.0";
    }

    public String getDescription() {
        return "";
    }

    public String getLinkLabel() {
        return getPropertyString("label"); //get label from configured properties options
    }

    public String getHref() {
        return getPropertyString("href"); //Let system to handle to post to the same page
    }

    public String getTarget() {
        return "post";
    }

    public String getHrefParam() {
        return getPropertyString("hrefParam");  //Let system to set the parameter to the checkbox name
    }

    public String getHrefColumn() {
        return getPropertyString("hrefColumn"); //Let system to set the primary key column of the binder
    }

    public String getConfirmation() {
        return getPropertyString("confirmation"); //get confirmation from configured properties options
    }

    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        // only allow POST
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
            return result;
        }
        
        // check for submited rows
        if (rowKeys != null && rowKeys.length > 0) {
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            JobDefinitionLogDao jobDefinitionLogDao = (JobDefinitionLogDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionLogDao");
            for (String r : rowKeys) {
                JobDefinition job = jobDefinitionDao.get(r);
                SchedulerUtil.deleteJob(job);
                jobDefinitionLogDao.deleteByJobId(r);
                jobDefinitionDao.delete(r);
            }
        }
        
        return result;
    }

    public String getLabel() {
        return "SchedulerDeleteAction";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return "";
    }
}
