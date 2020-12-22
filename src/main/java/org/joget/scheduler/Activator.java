package org.joget.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.commons.util.HostManager;
import static org.joget.scheduler.SchedulerUtil.isDefaultProfile;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(SchedulerMenu.class.getName(), new SchedulerMenu(), null));
        
        if ((HostManager.isVirtualHostEnabled() && isDefaultProfile()) || !HostManager.isVirtualHostEnabled()) {    
            SchedulerUtil.initSchedulers();
        }
    }

    public void stop(BundleContext context) {
        if ((HostManager.isVirtualHostEnabled() && isDefaultProfile()) || !HostManager.isVirtualHostEnabled()) {
            SchedulerUtil.stop();
        }
        
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}