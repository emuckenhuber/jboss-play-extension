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

package org.jboss.extension.play.api;

import java.io.File;
import java.util.Properties;

import org.infinispan.Cache;

import play.cache.CacheImpl;

/**
 * Application specific configuration
 *
 * @author Emanuel Muckenhuber
 */
public interface PlayApplicationConfiguration {

    /**
     * Get the application name.
     *
     * @return the application name
     */
    String getName();

    /**
     * Get the application root.
     *
     * @return the application root
     */
    File getApplicationRoot();

    /**
     * Get the context root.
     *
     * @return the conext root
     */
    String getContextRoot();

    /**
     * Get the configuration properties.
     *
     * @return the configuration properties
     */
    Properties getConfigurationProperties();

    /**
     * Get the cache.
     *
     * @return the cache, {@code null} if not configured.
     */
    Cache<String, Object> getCache();

}
