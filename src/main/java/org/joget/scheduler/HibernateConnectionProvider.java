package org.joget.scheduler;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.quartz.utils.ConnectionProvider;

public class HibernateConnectionProvider implements ConnectionProvider {
    public void initialize() throws SQLException {
        
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        LogUtil.debug(HibernateConnectionProvider.class.getName(), "Quartz getting connection...");
        DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
        Connection connection = ds.getConnection();
        return connection;
    }
 
    @Override
    public void shutdown() throws SQLException {

    }
}