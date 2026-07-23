package org.joget.scheduler;

import java.io.IOException;
import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.dao.JobDefinitionLogDao;
import org.joget.scheduler.model.JobDefinition;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchedulerWebService extends ExtDefaultPlugin implements PluginWebSupport {

    public String getName() {
        return "Scheduler Web Service";
    }

    public String getVersion() {
        return "8.0.2";
    }

    public String getDescription() {
        return "Web service endpoints for managing scheduler jobs";
    }

    public String getLabel() {
        return "Scheduler Web Service";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return "";
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorized(response)) {
            return;
        }

        String action = request.getParameter("action");

        if ("jobs".equals(action)) {
            if (!requireMethod(request, response, "GET")) {
                return;
            }
            getJobs(response);
        } else if ("job".equals(action)) {
            if (!requireMethod(request, response, "GET")) {
                return;
            }
            getJob(request, response);
        } else if ("delete".equals(action)) {
            if (!requireMethod(request, response, "DELETE")) {
                return;
            }
            deleteJob(request, response);
        } else if ("enable".equals(action)) {
            if (!requireMethod(request, response, "POST")) {
                return;
            }
            enableJob(request, response);
        } else if ("disable".equals(action)) {
            if (!requireMethod(request, response, "POST")) {
                return;
            }
            disableJob(request, response);
        } else if ("threadCount".equals(action)) {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                getThreadCount(response);
            } else if ("PUT".equalsIgnoreCase(request.getMethod())) {
                updateThreadCount(request, response);
            } else {
                response.setHeader("Allow", "GET, PUT");
                writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "HTTP GET or PUT is required");
            }
        } else {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Unsupported or missing action");
        }
    }

    protected boolean isAuthorized(HttpServletResponse response) throws IOException {
        if (WorkflowUtil.isCurrentUserAnonymous()) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Joget Scheduler\"");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
            return false;
        }

        if (!WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN")) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Administrator access is required");
            return false;
        }

        return true;
    }

    protected void getJobs(HttpServletResponse response) throws IOException {
        Collection<JobDefinition> jobs = getJobDefinitionDao().find("", null, "name", false, null, null);
        JSONArray result = new JSONArray();
        if (jobs != null) {
            for (JobDefinition job : jobs) {
                result.put(toJson(job));
            }
        }
        writeJson(response, HttpServletResponse.SC_OK, result);
    }

    protected void getJob(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId = getRequiredJobId(request, response);
        if (jobId == null) {
            return;
        }

        JobDefinition job = getJobDefinitionDao().get(jobId);
        if (job == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND,"Scheduler job not found");
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, toJson(job));
    }

    protected void deleteJob(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId = getRequiredJobId(request, response);
        if (jobId == null) {
            return;
        }

        JobDefinitionDao jobDefinitionDao = getJobDefinitionDao();
        JobDefinition job = jobDefinitionDao.get(jobId);
        if (job == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Scheduler job not found");
            return;
        }

        SchedulerUtil.deleteJob(job);
        getJobDefinitionLogDao().deleteByJobId(jobId);
        jobDefinitionDao.delete(jobId);

        JSONObject result = new JSONObject();
        result.put("message", "Scheduler job deleted");
        result.put("jobId", jobId);
        writeJson(response, HttpServletResponse.SC_OK, result);
    }

    protected void enableJob(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId = getRequiredJobId(request, response);
        if (jobId == null) {
            return;
        }

        JobDefinitionDao jobDefinitionDao = getJobDefinitionDao();
        JobDefinition job = jobDefinitionDao.get(jobId);
        if (job == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Scheduler job not found");
            return;
        }
        if (job.isEnabled()) {
            writeError(response, HttpServletResponse.SC_CONFLICT, "Scheduler job is already enabled");
            return;
        }

        job.setEnabled(true);
        SchedulerUtil.resumeJob(job);
        jobDefinitionDao.save(job);
        writeJson(response, HttpServletResponse.SC_OK, toJson(job));
    }

    protected void disableJob(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId = getRequiredJobId(request, response);
        if (jobId == null) {
            return;
        }

        JobDefinitionDao jobDefinitionDao = getJobDefinitionDao();
        JobDefinition job = jobDefinitionDao.get(jobId);
        if (job == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Scheduler job not found");
            return;
        }
        if (!job.isEnabled()) {
            writeError(response, HttpServletResponse.SC_CONFLICT, "Scheduler job is already disabled");
            return;
        }

        job.setEnabled(false);
        SchedulerUtil.pauseJob(job);
        jobDefinitionDao.save(job);
        writeJson(response, HttpServletResponse.SC_OK, toJson(job));
    }

    protected void getThreadCount(HttpServletResponse response) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, buildThreadCountResponse(null));
    }

    protected void updateThreadCount(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject body;
        try {
            body = readJsonBody(request);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON request body");
            return;
        }

        if (!body.has("threadCount")) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "threadCount is required");
            return;
        }

        int threadCount;
        try {
            String value = String.valueOf(body.get("threadCount")).trim();
            if (!value.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            threadCount = Integer.parseInt(value);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "threadCount must be a whole number between "
                    + SchedulerSettings.MIN_THREAD_COUNT + " and "
                    + SchedulerSettings.MAX_THREAD_COUNT);
            return;
        }

        if (!SchedulerSettings.isValidThreadCount(threadCount)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "threadCount must be between "
                    + SchedulerSettings.MIN_THREAD_COUNT + " and "
                    + SchedulerSettings.MAX_THREAD_COUNT);
            return;
        }

        SchedulerSettings.setConfiguredThreadCount(threadCount);
        boolean restartRequired = threadCount != SchedulerSettings.getEffectiveThreadCount();
        String message = restartRequired
                ? "Thread count saved. Restart each Joget node to apply the change."
                : "Thread count saved. The scheduler is already using this value.";
        writeJson(response, HttpServletResponse.SC_OK, buildThreadCountResponse(message));
    }

    protected JSONObject readJsonBody(HttpServletRequest request) throws IOException {
        StringBuilder json = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        if (json.length() == 0) {
            throw new IllegalArgumentException("Request body is empty");
        }
        return new JSONObject(json.toString());
    }

    protected JSONObject buildThreadCountResponse(String message) {
        int configuredThreadCount = SchedulerSettings.getConfiguredThreadCount();
        int effectiveThreadCount = SchedulerSettings.getEffectiveThreadCount();
        JSONObject result = new JSONObject();
        if (message != null) {
            result.put("message", message);
        }
        result.put("configuredThreadCount", configuredThreadCount);
        result.put("effectiveThreadCount", effectiveThreadCount);
        result.put("restartRequired", configuredThreadCount != effectiveThreadCount);
        return result;
    }

    protected String getRequiredJobId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobId = request.getParameter("id");
        if (jobId == null || jobId.trim().isEmpty()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Job ID is required");
            return null;
        }
        return jobId.trim();
    }

    protected boolean requireMethod(HttpServletRequest request, HttpServletResponse response, String method) throws IOException {
        if (!method.equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Allow", method);
            writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "HTTP " + method + " is required");
            return false;
        }
        return true;
    }

    protected JSONObject toJson(JobDefinition job) {
        JSONObject json = new JSONObject();
        json.put("id", job.getId());
        json.put("name", job.getName());
        json.put("appId", job.getAppId());
        json.put("pluginClass", job.getPluginClass());
        json.put("pluginProperties", job.getPluginProperties());
        json.put("trigger", job.getTrigger());
        json.put("nextFireTime", formatDate(job.getNextFireTime()));
        json.put("enabled", job.isEnabled());
        return json;
    }

    protected Object formatDate(Date date) {
        if (date == null) {
            return JSONObject.NULL;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    protected void writeError(HttpServletResponse response, int status, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("message", message);
        writeJson(response, status, error);
    }

    protected void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body.toString());
    }

    protected JobDefinitionDao getJobDefinitionDao() {
        return (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
    }

    protected JobDefinitionLogDao getJobDefinitionLogDao() {
        return (JobDefinitionLogDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionLogDao");
    }
}
