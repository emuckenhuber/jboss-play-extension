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

import java.util.Properties;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.extension.play.api.PlayFrameworkConfiguration;
import org.jboss.vfs.VirtualFile;

/**
 * Internal DUP attachment containing some Play! application specific properties.
 *
 * @author Emanuel Muckenhuber
 */
final class PlayApplication {

    public static final String CONTEXT_ROOT = "context-root";

    static final AttachmentKey<PlayApplication> APPLICATION_KEY = AttachmentKey.create(PlayApplication.class);
    static final AttachmentKey<PlayFrameworkConfiguration> FRAMEWORK_KEY = AttachmentKey.create(PlayFrameworkConfiguration.class);

    private final VirtualFile root;
    private final Properties configuration;

    public PlayApplication(final VirtualFile root, final Properties configuration) {
        this.root = root;
        this.configuration = configuration;
    }

    String getName() {
        return root.getName();
    }

    VirtualFile getApplicationRoot() {
        return root;
    }

    Properties getConfiguration() {
        return this.configuration;
    }

    String getContextPath() {
        return "/" + configuration.getProperty(CONTEXT_ROOT, contextName(root.getName()));
    }

    static String contextName(String name) {
        int i = name.indexOf('.');
        if(i == -1) {
            return name;
        } else {
            return name.substring(0, i);
        }
    }

}
