package org.joget.scheduler;

import org.joget.apps.app.service.AppUtil;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class AppContext {

    private static AppContext instance;
    private AbstractApplicationContext appContext;

    public synchronized static AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }
    
    public final void initialInstance() {
        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(this.getClass().getClassLoader());
            this.appContext = new GenericXmlApplicationContext();
            ((GenericXmlApplicationContext)this.appContext).setValidating(false);
            ((GenericXmlApplicationContext)this.appContext).load("/smApplicationContext.xml");
            ((GenericXmlApplicationContext)this.appContext).setParent(AppUtil.getApplicationContext());
            ((GenericXmlApplicationContext)this.appContext).refresh();
        } finally {
            currentThread.setContextClassLoader(threadContextClassLoader);
        }
    }

    private AppContext() {
        initialInstance();
    }

    public AbstractApplicationContext getAppContext() {
        return appContext;
    }
}
