/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.extension.play.deployment;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.infinispan.Cache;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.extension.play.api.PlayApplicationConfiguration;
import org.jboss.extension.play.api.PlayApplicationLifecycle;
import org.jboss.extension.play.api.PlayFrameworkConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service initializing a play app.
 *
 * @author Emanuel Muckenhuber
 */
class PlayApplicationService implements Service<Context> {

    private final PlayApplication application;
    private final PlayFrameworkConfiguration configuration;
    final InjectedValue<VirtualHost> hostInjector = new InjectedValue<VirtualHost>();
    final InjectedValue<Cache<String, Object>> cacheInjector = new InjectedValue<Cache<String, Object>>();
    private final ClassLoader cl;

    private PlayApplicationLifecycle lifecycle;
    private StandardContext context;

    PlayApplicationService(final PlayApplication application, final PlayFrameworkConfiguration configuration, final ClassLoader cl) {
        this.application = application;
        this.configuration = configuration;
        this.cl = cl;
    }

    @Override
    public synchronized void start(final StartContext start) throws StartException {
        final String contextPath = application.getContextPath();
        final StandardContext context = new StandardContext();
        try {
            final File applicationRoot = application.getApplicationRoot().getPhysicalFile();
            context.setDocBase(applicationRoot.getAbsolutePath());
            context.addLifecycleListener(new ContextConfig());

            // Not pretty, but configure the app
            final Class<?> clazz = cl.loadClass("org.jboss.play.plugin.PlayApplicationInitializer");
            lifecycle = PlayApplicationLifecycle.class.cast(clazz.newInstance());
            lifecycle.start(new PlayApplicationConfiguration() {
                @Override
                public String getName() {
                    return application.getName();
                }
                @Override
                public String getContextRoot() {
                    return contextPath;
                }
                @Override
                public Properties getConfigurationProperties() {
                    return application.getConfiguration();
                }
                @Override
                public Cache<String, Object> getCache() {
                    return cacheInjector.getOptionalValue();
                }
                @Override
                public File getApplicationRoot() {
                    return applicationRoot;
                }
            }, configuration);

            final Loader loader = new WebCtxLoader(this.cl);
            final Host host = hostInjector.getValue().getHost();
            loader.setContainer(host);
            context.setLoader(loader);
            context.setInstanceManager(new PlayInstanceManager());
            context.setIgnoreAnnotations(true);
            context.setPath(contextPath);

            final Wrapper wrapper = context.createWrapper();
            wrapper.setName("play");
            wrapper.setServletClass("org.jboss.play.plugin.PlayServlet");

            context.addChild(wrapper);
            context.addServletMapping("/", "play");

            host.addChild(context);
            context.create();
        } catch(Exception e) {
            throw new StartException(e);
        }
        try {
            context.start();
        } catch (LifecycleException e) {
            throw new StartException("failed to start context", e);
        }
        this.context = context;
    }

    @Override
    public synchronized  void stop(final StopContext stop) {
        try {
            hostInjector.getValue().getHost().removeChild(context);
            context.stop();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
        try {
            context.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        lifecycle.stop();
    }

    @Override
    public synchronized Context getValue() throws IllegalStateException, IllegalArgumentException {
        final Context context = this.context;
        if(context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    class PlayInstanceManager implements InstanceManager {

        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException,
                NamingException, InstantiationException, ClassNotFoundException {
            return newInstance(className, cl);
        }

        @Override
        public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException,
                NamingException, InstantiationException, ClassNotFoundException {
            return newInstance(classLoader.loadClass(fqcn));
        }

        @Override
        public Object newInstance(Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
            return c.newInstance();
        }

        @Override
        public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
            //
        }

        @Override
        public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {

        }

    }

}
