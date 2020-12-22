package org.joget.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.DynamicDataSourceManager;
import org.joget.commons.util.HostManager;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.model.JobDefinition;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import static org.quartz.JobKey.jobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulerUtil {
    private static final Map<String, Scheduler> schedulers = new HashMap<String, Scheduler>();
    
    public static void initSchedulers() {
        Thread initSchedulerThread = new PluginThread(new Runnable() {

            public void run() {
                try {
                    waitForDatasourceReady();
                    
                    if (HostManager.isVirtualHostEnabled() && isDefaultProfile()) {
                        LogUtil.debug(SchedulerUtil.class.getName(), "Initial schedulers start...");

                        // loop each profile
                        int count = 0;
                        Properties profiles = DynamicDataSourceManager.getProfileProperties();
                        Set<String> profileSet = new HashSet(profiles.values());
                        for (String profile : profileSet) {
                            if (profile.contains(",")) {
                                continue;
                            }
                            count++;
                            try {
                                LogUtil.debug(SchedulerUtil.class.getName(), count + ") Initial schedulers start for profile: " + profile);
                                HostManager.setCurrentProfile(profile);

                                AppContext.getInstance().initialInstance(); //force it to create tables if table not exist.

                                if (hasJob()) {
                                    getInstance();
                                }

                                LogUtil.info(SchedulerUtil.class.getName(), count + ") Initial schedulers done for profile: " + profile);
                            } finally {
                                HostManager.resetProfile();
                            }
                        }
                    } else {
                        LogUtil.debug(SchedulerUtil.class.getName(), "Initial schedulers start...");

                        if (hasJob()) {
                            getInstance();
                        }
                    }
                    LogUtil.debug(SchedulerUtil.class.getName(), "Initial schedulers done...");
                } catch (Exception ex) {
                    LogUtil.error(SchedulerUtil.class.getName(), ex, null);
                }
            }
        });
        initSchedulerThread.setDaemon(false);
        initSchedulerThread.start();
    }
    
    protected static void waitForDatasourceReady() {
        try {
            DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
            if (ds == null) {
                sleep(500);
                waitForDatasourceReady();
            }
        } catch (Exception e) {
            sleep(500);
            waitForDatasourceReady();
        }
    }
    
    protected static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {}
    }
    
    public static boolean hasJob() {
        //JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
        //return jobDefinitionDao.hasPendingJob(); 
        return true;
    }
    
    public static void setupDB(String name) {
        DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        InputStream in = null;
        try {
            con = ds.getConnection();
            
            // check for existing tables
            boolean exists = false;
            LogUtil.debug(SchedulerUtil.class.getName(), "Checking schedulers tables for " + name);
            try {
                stmt = con.createStatement();
                rs = stmt.executeQuery("SELECT * FROM QRTZ_TRIGGERS");
                exists = true;
            } catch (SQLException ex) {}
            if (!exists) {
                //test for lower case
                try {
                    stmt = con.createStatement();
                    rs = stmt.executeQuery("SELECT * FROM qrtz_triggers");
                    exists = true;
                } catch (SQLException ex) {}
            }
            
            if (!exists) {
                con.setAutoCommit(false);
                LogUtil.debug(SchedulerUtil.class.getName(), "Start create tables for " + name);
                String driver = BeanUtils.getProperty(ds, "driverClassName");
                String schemaFile = null;
                if (driver.equals("oracle.jdbc.driver.OracleDriver")) {
                    schemaFile = "/setup/tables_oracle.sql";
                } else if (driver.equals("com.mysql.jdbc.Driver")) {
                    schemaFile = "/setup/tables_mysql_innodb.sql";
                } else if (driver.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
                    schemaFile = "/setup/tables_sqlServer.sql";
                } else if (driver.equals("org.postgresql.Driver")) {
                    schemaFile = "/setup/tables_postgres.sql";
                } else {
                    throw new SQLException("Unrecognized database type, please create your quartz scheduler tables manually");
                }
                
                ScriptRunner runner = new ScriptRunner(con, false, true);
                in = SchedulerUtil.class.getResourceAsStream(schemaFile);
                runner.runScript(new BufferedReader(new InputStreamReader(in)));
                con.commit();
                LogUtil.debug(SchedulerUtil.class.getName(), "Completed create tables for " + name);
            } else {
                LogUtil.debug(SchedulerUtil.class.getName(), "Schedulers tables for " + name + " are existed");
            }
        } catch (Exception e) {
            LogUtil.error(SchedulerUtil.class.getName(), e, "Error create tables for " + name);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    // ignore
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    // ignore
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    // ignore
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
    
    public static String schedulerName() {
        if (HostManager.isVirtualHostEnabled()) {
            String name = HostManager.getCurrentProfile();
            if (name == null) {
                name = "default";
            }
            return "sch_" + name + "_schedulerMenu";
        } else {
            return "sch_schedulerMenu";
        }
    }
    
    public static Scheduler getInstance() {
        String name = schedulerName();
        return getInstance(name);
    }
    
    public static boolean isSchedulerExist() {
        String name = schedulerName();
        return schedulers.get(name) != null;
    }
    
    protected static Scheduler getInstance(String name) {
        Scheduler scheduler = schedulers.get(name);
        if (scheduler == null) {
            setupDB(name);
            try {
                StdSchedulerFactory sf = new StdSchedulerFactory();
                Properties properties = new Properties();
                properties.put("org.quartz.scheduler.instanceName", name);
                properties.put("org.quartz.threadPool.threadCount", "5");
                properties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
                
                DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
                String driver = BeanUtils.getProperty(ds, "driverClassName");
                if (driver.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
                    properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.MSSQLDelegate");
                } else if (driver.equals("org.postgresql.Driver")) {
                    properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
                } else {
                    properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
                }
                
                properties.put("org.quartz.jobStore.isClustered", "true");
                properties.put("org.quartz.jobStore.dataSource", "setupDataSource");
                properties.put("org.quartz.dataSource.setupDataSource.connectionProvider.class", "org.joget.scheduler.HibernateConnectionProvider");
        
                sf.initialize(properties);
                scheduler = sf.getScheduler();
                scheduler.start();
                
                schedulers.put(name, scheduler);
                LogUtil.debug(SchedulerUtil.class.getName(), "Scheduler : " + name + " is started");
            } catch (Exception e) {
                LogUtil.error(SchedulerUtil.class.getName(), e, null);
            }
        }
        return scheduler;
    }
    
    public static void scheduleJob(JobDefinition jobDefinition) {
        jobDefinition.setNextFireTime(null);
        try {
            Scheduler s = SchedulerUtil.getInstance();
            
            deleteJob(jobDefinition);

            JobDetail job = newJob(SchedulerJob.class)
            .withIdentity(jobDefinition.getId(), jobDefinition.getAppId())
            .build();

            Trigger trigger = newTrigger()
            .withIdentity(jobDefinition.getId(), jobDefinition.getAppId())
            .withSchedule(cronSchedule(jobDefinition.getTrigger()))
            .build();

            s.scheduleJob(job, trigger);
            
            JobKey key = jobKey(jobDefinition.getId(), jobDefinition.getAppId());
            if (s.checkExists(key)){
                JobDetail detail = s.getJobDetail(key);
                List<Trigger> triggers = (List<Trigger>) s.getTriggersOfJob(key);

                if (!triggers.isEmpty()) {
                    Date nextFireTime = triggers.iterator().next().getNextFireTime();
                    jobDefinition.setNextFireTime(nextFireTime);
                }
            }
        } catch (Exception e) {
            LogUtil.error(SchedulerUtil.class.getName(), e, null);
        }
    }
    
    public static void deleteJob(JobDefinition jobDefinition) {
        try {
            if (jobDefinition != null) {
                Scheduler s = SchedulerUtil.getInstance();
                s.deleteJob(jobKey(jobDefinition.getId(), jobDefinition.getAppId()));
            }
        } catch (Exception e) {
            LogUtil.error(SchedulerUtil.class.getName(), e, null);
        }
    }
    
    public static void stop() {
        LogUtil.debug(SchedulerUtil.class.getName(), "Stoping schedulers start...");
        if (HostManager.isVirtualHostEnabled() && isDefaultProfile()) {
            for (Scheduler scheduler: schedulers.values()) {
                try {
                    scheduler.shutdown();
                    LogUtil.debug(SchedulerUtil.class.getName(), "Scheduler : " + scheduler.getSchedulerName() + " is stopped");
                } catch (Exception e) {
                    LogUtil.error(SchedulerUtil.class.getName(), e, null);
                }
            }
        } else {
            try {
                String name = schedulerName();
                Scheduler scheduler = schedulers.get(name);
                if (scheduler != null) {
                    scheduler.shutdown();
                    LogUtil.debug(SchedulerUtil.class.getName(), "Scheduler : " + scheduler.getSchedulerName() + " is stopped");
                }
            } catch (Exception e) {
                LogUtil.error(SchedulerUtil.class.getName(), e, null);
            }
        }
        LogUtil.debug(SchedulerUtil.class.getName(), "Stoping schedulers done...");
    }
    
    public final static boolean isDefaultProfile() {
        boolean isDefaultProfile = false;
        Properties props = DynamicDataSourceManager.getProfileProperties();
        String defaultProfileValue = props.getProperty("currentProfile");
        LogUtil.debug(SchedulerUtil.class.getName(), "default >>> " + defaultProfileValue);
        if (defaultProfileValue != null && !defaultProfileValue.isEmpty()) {
            String currentProfile = DynamicDataSourceManager.getCurrentProfile();
            LogUtil.debug(SchedulerUtil.class.getName(), "current >>> " + currentProfile);
            isDefaultProfile = (currentProfile != null && currentProfile.equals(defaultProfileValue));
        }
        return isDefaultProfile;
    }
}
