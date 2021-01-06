package org.joget.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListAction;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.CsvUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.model.JobDefinition;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class SchedulerMenu extends UserviewMenu implements PluginWebSupport {
    public final static String MESSAGE_PATH = "messages/SchedulerMenu";
    
    private DataList cacheDataList = null;
    
    @Override
    public String getCategory() {
        return "Enterprise";
    }

    @Override
    public String getIcon() {
        return "/plugin/org.joget.apps.userview.lib.InboxMenu/images/grid_icon.gif";
    }

    @Override
    public String getRenderPage() {
        return null;
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        return null;
    }

    public String getName() {
        return "Scheduler";
    }

    public String getVersion() {
        return "6.0.1";
    }

    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.scheduler.SchedulerMenu.pluginLabel", getClassName(), MESSAGE_PATH);
    }
     
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.scheduler.SchedulerMenu.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/userview/schedulerMenu.json", null, true, MESSAGE_PATH);
    }
    
    @Override
    public String getJspPage() {
        if (!SchedulerUtil.isSchedulerExist()) {    
            setProperty("view", "formView");
            setProperty("formHtml", "<p>Sorry, this feature is not supported in your installation.</p>");
            return "userview/plugin/form.jsp";
        }
        
        String mode = getRequestParameterString("_mode");
        
        if ("add".equals(mode) || "edit".equals(mode)) {
            setProperty("customHeader", getPropertyString(mode + "-customHeader"));
            setProperty("customFooter", getPropertyString(mode + "-customFooter"));
            setProperty("messageShowAfterComplete", getPropertyString(mode + "-messageShowAfterComplete"));
            return handleForm();
        } else if ("log".equals(mode)) {
            setProperty("customHeader", getPropertyString("list-customHeader"));
            setProperty("customFooter", getPropertyString("list-customFooter"));
            return handleList("log");
        } else {
            setProperty("customHeader", getPropertyString("list-customHeader"));
            setProperty("customFooter", getPropertyString("list-customFooter"));
            return handleList(null);
        }
    }
    
    protected String addParamToUrl(String url, String name, String value) {
        return StringUtil.addParamsToUrl(url, name, value);
    }

    protected String handleList(String type) {
        viewList(type);
        return "userview/plugin/datalist.jsp";
    }

    protected void viewList(String type) {
        try {
            // get data list
            DataList dataList = getDataList(type);
            
            if (dataList != null) {
                
                //overide datalist result to use userview result
                DataListActionResult ac = dataList.getActionResult();
                if (ac != null) {
                    if (ac.getMessage() != null && !ac.getMessage().isEmpty()) {
                        setAlertMessage(ac.getMessage());
                    }
                    if (ac.getType() != null && DataListActionResult.TYPE_REDIRECT.equals(ac.getType()) &&
                            ac.getUrl() != null && !ac.getUrl().isEmpty()) {
                        if ("REFERER".equals(ac.getUrl())) {
                            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                            if (request != null && request.getHeader("Referer") != null) {
                                setRedirectUrl(request.getHeader("Referer"));
                            } else {
                                setRedirectUrl("REFERER");
                            }
                        } else {
                            if (ac.getUrl().startsWith("?")) {
                                ac.setUrl(getUrl() + ac.getUrl());
                            }
                            setRedirectUrl(ac.getUrl());
                        }
                    }
                }

                // set data list
                setProperty("dataList", dataList);
            }
        } catch (Exception ex) {
            StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message += "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            setProperty("error", message);
        }
    }

    protected DataList getDataList(String type) throws BeansException {
        if (cacheDataList == null) {
            // get datalist
            DataListService dataListService = (DataListService) AppUtil.getApplicationContext().getBean("dataListService");
            String json = null;
            
            if ("log".equals(type)) {
                json = AppUtil.readPluginResource(getClass().getName(), "/properties/userview/schedulerLogDatalist.json", null, true, MESSAGE_PATH);
            } else {
                String newLabel = (getPropertyString("list-newButtonLabel") != null && getPropertyString("list-newButtonLabel").trim().length() > 0) ? getPropertyString("list-newButtonLabel") : ResourceBundleUtil.getMessage("userview.crudmenu.button.new");
                json = AppUtil.readPluginResource(getClass().getName(), "/properties/userview/schedulerDatalist.json", new String[]{newLabel}, true, MESSAGE_PATH);
            }
            
            cacheDataList = dataListService.fromJson(json);
            cacheDataList.setDisableQuickEdit(true);
            
            if ("log".equals(type)) {
                String id = getRequestParameterString("jobid");
                cacheDataList.setBinder(new SchedulerDatalistBinder(id));
                cacheDataList.setCheckboxPosition("no");
            } else {
                cacheDataList.setBinder(new SchedulerDatalistBinder());
                cacheDataList.setActionPosition(getPropertyString("buttonPosition"));
                cacheDataList.setSelectionType(getPropertyString("selectionType"));
                cacheDataList.setCheckboxPosition(getPropertyString("checkboxPosition"));
                cacheDataList = addDatalistButtons(cacheDataList);

                Collection<DataListAction> actionList = new ArrayList<DataListAction>();
                actionList.addAll(Arrays.asList(cacheDataList.getActions()));
                DataListAction action = new SchedulerDeleteAction();
                String deleteLabel = (getPropertyString("list-deleteButtonLabel") != null && getPropertyString("list-deleteButtonLabel").trim().length() > 0) ? getPropertyString("list-deleteButtonLabel") : ResourceBundleUtil.getMessage("general.method.label.delete");
                Map properties = new HashMap();
                properties.put("id", "delete_job");
                properties.put("label", deleteLabel);
                properties.put("confirmation", AppPluginUtil.getMessage("userview.scheduler.delete", getClassName(), MESSAGE_PATH));
                action.setProperties(properties);
                actionList.add(action);
                cacheDataList.setActions((DataListAction[]) actionList.toArray(new DataListAction[actionList.size()]));
            }
        }
        return cacheDataList;
    }

    protected DataList addDatalistButtons(DataList dataList) {

        //Add edit row action
        Map editProperties = new HashMap();
        editProperties.put("id", "edit_job");
        editProperties.put("label", (getPropertyString("list-editLinkLabel") != null && getPropertyString("list-editLinkLabel").trim().length() > 0) ? getPropertyString("list-editLinkLabel") : ResourceBundleUtil.getMessage("general.method.label.edit"));
        editProperties.put("href", addParamToUrl(getUrl(), "_mode", "edit"));
        editProperties.put("hrefParam", "id");
        String primaryKeyColumn = "id";
        DataListBinder binder = dataList.getBinder();
        if (binder != null) {
            primaryKeyColumn = binder.getPrimaryKeyColumnName();
        }
        if (primaryKeyColumn == null) {
            primaryKeyColumn = "id";
        }
        editProperties.put("hrefColumn", primaryKeyColumn);
        dataList.addDataListAction("org.joget.apps.datalist.lib.HyperlinkDataListAction", DataList.DATALIST_ROW_ACTION, editProperties);
        
        //Add log row action
        Map logProperties = new HashMap();
        logProperties.put("id", "job_log");
        logProperties.put("label", AppPluginUtil.getMessage("userview.scheduler.logs", getClassName() , MESSAGE_PATH));
        logProperties.put("href", addParamToUrl(addParamToUrl(getUrl(), "_mode", "log"), "embed", "true"));
        logProperties.put("hrefParam", "jobid");
        logProperties.put("hrefColumn", primaryKeyColumn);
        logProperties.put("target", "popup");
        dataList.addDataListAction("org.joget.apps.datalist.lib.HyperlinkDataListAction", DataList.DATALIST_ROW_ACTION, logProperties);
        
        return dataList;
    }

    protected String handleForm() {
        if ("submit".equals(getRequestParameterString("_action"))) {
            // only allow POST
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
                return "userview/plugin/unauthorized.jsp";
            }
            
            // submit form
            submitForm();
        } else {
            displayForm();

        }
        return "userview/plugin/form.jsp";
    }

    protected void displayForm() {

        String id = getRequestParameterString(FormUtil.PROPERTY_ID);

        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");

        Form form = null;
        FormData formData = new FormData();

        form = retrieveDataForm(formData, id);

        if (form != null) {
            // generate form HTML
            String formHtml = formService.retrieveFormHtml(form, formData);
            setProperty("view", "formView");
            setProperty("formHtml", formHtml);
        }
    }

    protected void submitForm() {

        String id = getRequestParameterString(FormUtil.PROPERTY_ID);

        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");

        Form form = null;
        FormData formData = new FormData();

        form = retrieveDataForm(formData, id);

        // submit data form
        form = submitDataForm(formData, form);

        if (form != null) {
            // generate form HTML
            String formHtml = null;

            // check for validation errors
            Map<String, String> errors = formData.getFormErrors();
            int errorCount = 0;
            if (!formData.getStay() && (errors == null || errors.isEmpty())) {
                String mode = getRequestParameterString("_mode");
                String redirectUrl = getPropertyString("redirectUrlAfterComplete");
                setRedirectUrl(redirectUrl);
                setAlertMessage(getPropertyString(mode + "-messageShowAfterComplete"));
                // render normal template
                formHtml = formService.generateElementHtml(form, formData);
            } else {
                // render error template
                formHtml = formService.generateElementErrorHtml(form, formData);
                errorCount = errors.size();
            }
            
            if (formData.getStay()) {
                setAlertMessage("");
                setRedirectUrl("");
            }

            // show form
            setProperty("view", "formView");
            setProperty("stay", formData.getStay());
            setProperty("submitted", Boolean.TRUE);
            setProperty("errorCount", errorCount);
            setProperty("formHtml", formHtml);
        }
    }

    protected Form retrieveDataForm(FormData formData, String primaryKeyValue) {
        Form form = null;
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        String mode = getRequestParameterString("_mode");

        Map requestParams = getRequestParameters();
        requestParams.put("_mode", getPropertyString("mode"));
        
        // set primary key
        formData.setPrimaryKeyValue(primaryKeyValue);
        formData = formService.retrieveFormDataFromRequestMap(formData, requestParams);

        // retrieve form
        String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit";

        if (primaryKeyValue != null) {
            try {
                formUrl += "&" + FormUtil.PROPERTY_ID + "=" + URLEncoder.encode(primaryKeyValue, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                // ignore
            }
        }

        String submitLabel = ResourceBundleUtil.getMessage("general.method.label.save");
        String cancelLabel = null;

        if ("add".equals(mode)) {
            if (getPropertyString("add-saveButtonLabel") != null && getPropertyString("add-saveButtonLabel").trim().length() > 0) {
                submitLabel = getPropertyString("add-saveButtonLabel");
            }
            cancelLabel = ResourceBundleUtil.getMessage("general.method.label.cancel");
            if (getPropertyString("add-cancelButtonLabel") != null && getPropertyString("add-cancelButtonLabel").trim().length() > 0) {
                cancelLabel = getPropertyString("add-cancelButtonLabel");
            }
        } else if ("edit".equals(mode)) {
            if (getPropertyString("edit-saveButtonLabel") != null && getPropertyString("edit-saveButtonLabel").trim().length() > 0) {
                submitLabel = getPropertyString("edit-saveButtonLabel");
            }
            cancelLabel = ResourceBundleUtil.getMessage("general.method.label.back");
            if (getPropertyString("edit-backButtonLabel") != null && getPropertyString("edit-backButtonLabel").trim().length() > 0) {
                cancelLabel = getPropertyString("edit-backButtonLabel");
            }
        }
        
        String formJson = AppUtil.readPluginResource(getClass().getName(), "/properties/userview/schedulerForm.json", new Object[]{getServiceUrl()}, true, MESSAGE_PATH);
        formJson = AppUtil.processHashVariable(formJson, null, StringUtil.TYPE_JSON, null);
        form = (Form) formService.loadFormFromJson(formJson, formData); 
        
        if (form != null) {
            SchedulerFormBinder binder = new SchedulerFormBinder();
            form.setLoadBinder(binder);
            form.setStoreBinder(binder);
            form = formService.loadFormData(form, formData);
            form.setProperty("removeQuickEdit", "true");
            form = appService.viewDataForm(form, null, submitLabel, cancelLabel, "window", formData, formUrl, getUrl());
        
            // make primary key read-only
            Element el = FormUtil.findElement(FormUtil.PROPERTY_ID, form, formData);
            if (el != null) {
                String idValue = FormUtil.getElementPropertyValue(el, formData);
                if (idValue != null && !idValue.trim().isEmpty() && !"".equals(formData.getRequestParameter(FormUtil.FORM_META_ORIGINAL_ID))) {
                    el.setProperty(FormUtil.PROPERTY_READONLY, "true");
                }
            }
        }

        // set form to read-only if required
        if ("edit".equals(mode)) {
            Boolean readonly = "yes".equalsIgnoreCase(getPropertyString("edit-readonly"));
            Boolean readonlyLabel = "true".equalsIgnoreCase(getPropertyString("edit-readonlyLabel"));
            if (readonly || readonlyLabel) {
                FormUtil.setReadOnlyProperty(form, readonly, readonlyLabel);
            }
        }
        return form;
    }

    /**
     * Handles data form submission.
     */
    protected Form submitDataForm(FormData formData, Form form) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        formData = formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());
        formData = formService.executeFormActions(form, formData);

        setProperty("submitted", Boolean.TRUE);

        String redirectUrl = getUrl();
        setProperty("redirectUrlAfterComplete", redirectUrl);

        return form;
    }

    protected Map getHyperlinkDataListActionProperties(String label, String href, String hrefParam, String hrefColumn, String confirmation, boolean visible) {
        Map actionProperties = new HashMap();
        actionProperties.put("id", "LR_" + label.replace(" ", "_"));
        actionProperties.put("label", label);
        actionProperties.put("href", href);
        actionProperties.put("hrefParam", hrefParam);
        actionProperties.put("hrefColumn", hrefColumn);
        actionProperties.put("confirmation", confirmation);
        actionProperties.put("visible", Boolean.toString(visible));
        return actionProperties;
    }
    
    protected String getServiceUrl() {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String userviewId = getUserview().getPropertyString("id");
        String key = (getKey() != null)? getKey() : "";
        String menuId = getPropertyString("customId");
        if (menuId == null || menuId.trim().isEmpty()) {
            menuId = getPropertyString("id");
        }
        String nonce = SecurityUtil.generateNonce(new String[]{"SchudulerMenu", appDef.getId(), appDef.getVersion().toString(), userviewId, key, menuId}, 3);
        try {
            nonce = URLEncoder.encode(nonce, "UTF-8");
        } catch (Exception e) {}
        
        String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/"+appDef.getId()+"/"+appDef.getVersion()+"/plugin/org.joget.scheduler.SchedulerMenu/service?userviewId=" + userviewId + "&menuId=" + menuId + "&key=" + key + "&_nonce=" + nonce;
        return url;
    }
    
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        
        try {
            String userviewId = request.getParameter("userviewId");
            String key = request.getParameter("key");
            String menuId = request.getParameter("menuId");
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            
            String nonce = request.getParameter("_nonce");
            if (!SecurityUtil.verifyNonce(nonce, new String[]{"SchudulerMenu", appDef.getAppId(), appDef.getVersion().toString(), userviewId, key, menuId})) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            if ("config".equals(action)) {
                String submit = request.getParameter("submit");
                String pluginClass = request.getParameter("pluginClass");
                String pluginProperties = request.getParameter("pluginProp");
                
                if ("post".equalsIgnoreCase(request.getMethod()) && "true".equals(submit)) {
                    pluginProperties = request.getParameter("pluginProperties");
                    write("<script>window.parent.updateProps(\""+StringUtil.escapeString(pluginProperties, StringUtil.TYPE_JSON, null)+"\");</script>", response);
                } else {
                    AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                    PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
                    
                    AppDefinition selectedAppDef = appService.getPublishedAppDefinition(request.getParameter("applicationId"));
                    Plugin plugin = pluginManager.getPlugin(pluginClass);
                    
                    Map<String, Object> modelMap = new HashMap<String, Object>();
                    modelMap.put("properties", pluginProperties);
                    
                    if (plugin != null) {
                        modelMap.put("propertyEditable", (PropertyEditable) plugin);
                        modelMap.put("plugin", plugin);
                        
                        PluginDefaultProperties pluginDefaultProperties = pluginDefaultPropertiesDao.loadById(pluginClass, selectedAppDef);

                        if (pluginDefaultProperties != null) {
                            if (!(plugin instanceof PropertyEditable)) {
                                Map defaultPropertyMap = new HashMap();

                                String properties = pluginDefaultProperties.getPluginProperties();
                                if (properties != null && properties.trim().length() > 0) {
                                    defaultPropertyMap = CsvUtil.getPluginPropertyMap(properties);
                                }
                                modelMap.put("defaultPropertyMap", defaultPropertyMap);
                            } else {
                                modelMap.put("defaultProperties", PropertyUtil.propertiesJsonLoadProcessing(pluginDefaultProperties.getPluginProperties()));
                            }
                        }
                    }

                    String url = "?"+request.getQueryString() + "&submit=true&action=config";

                    //update nonce in url
                    url = StringUtil.addParamsToUrl(url, "_nonce", nonce);

                    modelMap.put("actionUrl", url);

                    String content = UserviewUtil.renderJspAsString("console/plugin/pluginConfig.jsp", modelMap);
                    content = fixI18nForNoneAdminUser(content);
                    write(fixMissingLabel(content), response);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
    
    protected String fixI18nForNoneAdminUser(String content) {
        if(!WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN")) {
            Pattern pattern = Pattern.compile("<script type=\\\"text/javascript\\\" src=\\\".+/web/console/i18n/peditor.+\\\"></script>");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String script = matcher.group();
                
                Properties keys = new Properties();
                //get message key from property file
                InputStream inputStream = null;
                try {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("peditor.properties");
                    if (inputStream != null) {
                        keys.load(inputStream);

                        String replace = "<script>var peditor_lang = {";
                        for (Object k : keys.keySet()) {
                            replace += "'"+k.toString()+"' : '" + StringUtil.escapeString(keys.getProperty(k.toString()), StringUtil.TYPE_JSON, null) + "',";
                        }
                        replace += " lang_file_name : 'peditor.properties'}\n";
                        replace += "function get_peditor_msg(key){\n" +
                                   "    return (peditor_lang[key] !== undefined) ? peditor_lang[key] : '??'+key+'??';\n" +
                                   "}\n</script>";
                        
                        content = content.replaceAll(StringUtil.escapeRegex(script), StringUtil.escapeRegex(replace));
                    }
                } catch (Exception e) {
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
        return content;
    } 
    
    protected void write(String content, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.write(content);
    }
    
    protected String fixMissingLabel(String content) {
        content = content.replaceAll(StringUtil.escapeRegex("???"), StringUtil.escapeRegex("@@"));
        PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        content = pluginManager.processPluginTranslation(content, getClassName(), null);
        content = content.replaceAll(StringUtil.escapeRegex("@@"), StringUtil.escapeRegex("???"));
        
        return content;
    }
    
    public static void deleteJob(String jobId) {
        JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
        JobDefinition jobDefinition = jobDefinitionDao.get(jobId);
        if (jobDefinition != null) {
            SchedulerUtil.deleteJob(jobDefinition);
            jobDefinitionDao.delete(jobId);
        }
    }
}
