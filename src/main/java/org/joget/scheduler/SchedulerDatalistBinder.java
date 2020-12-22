package org.joget.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.FormRowDataListBinder;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListBinderDefault;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.commons.util.LogUtil;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.dao.JobDefinitionLogDao;

public class SchedulerDatalistBinder extends DataListBinderDefault {
    private JobDefinitionDao dao = null;
    private JobDefinitionLogDao logDao = null;
    private String jobId;
    
    SchedulerDatalistBinder() {
        
    }
    
    SchedulerDatalistBinder(String jobId) {
        this.jobId = jobId;
    }
    
    public String getName() {
        return "SchedulerDatalistBinder";
    }

    public String getVersion() {
        return "6.0.0";
    }

    public String getDescription() {
        return "";
    }

    public DataListColumn[] getColumns() {
        return null;
    }

    public String getPrimaryKeyColumnName() {
        return "id";
    }

    public String getLabel() {
        return "SchedulerDatalistBinder";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        return null;
    }
    
    @Override
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        alterOracleSession();
        DataListCollection resultList = new DataListCollection();

        try {
            DataListFilterQueryObject criteria = getCriteria(properties, filterQueryObjects);

            Collection data = null;
            if (jobId == null) {
                data = getDao().find(criteria.getQuery(), criteria.getValues(), sort, desc, start, rows);
            } else {
                data = getLogDao().findByJobId(jobId, criteria.getQuery(), criteria.getValues(), sort, desc, start, rows);
            }

            if (data != null & !data.isEmpty()) {
                resultList.addAll(data);
            }
            
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "");
        }

        return resultList;
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        alterOracleSession();
        int count = 0;
        
        try {
            DataListFilterQueryObject criteria = getCriteria(properties, filterQueryObjects);

            Long total = null;
            if (jobId == null) {
                total = getDao().count(criteria.getQuery(), criteria.getValues());
            } else {
                total = getLogDao().countByJobId(jobId, criteria.getQuery(), criteria.getValues());
            }

            if (total != null) {
                count = (int) (total + 0);
            }
            
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "");
        }
        
        return count;
    }
    
    @Override
    public String getColumnName(String name) {
        return "e." + name;
    }
    
    protected JobDefinitionDao getDao() {
        if (dao == null) {
            dao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
        }
        return dao;
    }
    
    protected JobDefinitionLogDao getLogDao() {
        if (logDao == null) {
            logDao = (JobDefinitionLogDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionLogDao");
        }
        return logDao;
    }
    
    protected DataListFilterQueryObject getCriteria(Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        Collection<String> params = new ArrayList<String>();
        String condition = "";

        DataListFilterQueryObject filter = processFilterQueryObjects(filterQueryObjects);

        if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
            condition = " WHERE " + filter.getQuery();
            if (filter.getValues() != null && filter.getValues().length > 0) {
                params.addAll(Arrays.asList(filter.getValues()));
            }
        }

        DataListFilterQueryObject queryObject = new DataListFilterQueryObject();
        queryObject.setQuery(condition);
        if (params.size() > 0) {
            queryObject.setValues((String[]) params.toArray(new String[0]));
        }
        return queryObject;
    }
    
    protected void alterOracleSession() {
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            String driver = BeanUtils.getProperty(ds, "driverClassName");
            
            if (driver.equals("oracle.jdbc.driver.OracleDriver")) {
                Connection con = null;
                PreparedStatement pstmt = null;
                try {
                    con = ds.getConnection();
                    pstmt = con.prepareStatement("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH:MI:SS.FF'");
                    pstmt.executeUpdate();
                } catch (Exception e) {
                } finally {
                    try {
                        if (pstmt != null) {
                            pstmt.close();
                        }
                    } catch(Exception e){}
                    try {
                        if (con != null) {
                            con.close();
                        }
                    } catch(Exception e){}
                }
            }
        } catch (Exception e) {
            LogUtil.error(FormRowDataListBinder.class.getName(), e, "");
        }
    }
}
