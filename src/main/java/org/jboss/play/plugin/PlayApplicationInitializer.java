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

package org.jboss.play.plugin;

import org.jboss.extension.play.api.PlayApplicationConfiguration;
import org.jboss.extension.play.api.PlayApplicationLifecycle;
import org.jboss.extension.play.api.PlayFrameworkConfiguration;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.cache.CacheImpl;
import play.mvc.Router;

/**
 * @author Emanuel Muckenhuber
 */
public class PlayApplicationInitializer implements PlayApplicationLifecycle {

    static {
        // j.u.L is handled by the log manager by default
        Logger.forceJuli = true;
    }

    @Override
    public void start(final PlayApplicationConfiguration application, final PlayFrameworkConfiguration configuration) {

        final String contextPath = application.getContextRoot();
        // Initialize play framework
        // Play.standalonePlayServer = false;
        Play.frameworkPath = configuration.getRoot();
        Play.ctxPath = contextPath;
        Play.configuration = application.getConfigurationProperties();
        // If configured use external infinispan cache
        createCacheImpl(application);
        // Init application
        Play.init(application.getApplicationRoot(), "jboss");

        Router.load(contextPath);
    }

    @Override
    public void stop() {
        Play.stop();
    }

    static CacheImpl createCacheImpl(final PlayApplicationConfiguration application) {
        final org.infinispan.Cache<String, Object> cache = application.getCache();
        if(cache != null) {
            Cache.forcedCacheImpl = new InfinispanCacheImpl(cache);
        }
        return null;
    }

}
