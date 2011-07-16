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

package org.jboss.extension.play;

import java.io.File;

import org.jboss.extension.play.api.PlayFrameworkConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
class PlayFrameworkService implements Service<PlayFrameworkConfiguration> {

    private PlayFrameworkConfiguration configuration;
    final InjectedValue<String> path = new InjectedValue<String>();

    @Override
    public synchronized void start(StartContext start) throws StartException {
        final File root = new File(path.getValue());
        if(! root.exists()) {
            throw new StartException("play root does not exist: " + path.getValue());
        }
        final File frameworkRoot = new File(root, Constants.FRAMEWORK);
        if(! frameworkRoot.exists()) {
            throw new StartException("framwork root does not exist: " + path.getValue());
        }
        final File libDir = new File(frameworkRoot, Constants.LIB);
        this.configuration = new PlayFrameworkConfiguration() {
            @Override
            public File getRoot() {
                return root;
            }
            @Override
            public File getFrameworkRoot() {
                return frameworkRoot;
            }
            @Override
            public File getFrameworkLib() {
                return libDir;
            }
        };
    }

    @Override
    public synchronized void stop(StopContext stop) {
        this.configuration = null;
    }

    @Override
    public synchronized PlayFrameworkConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        final PlayFrameworkConfiguration configuration = this.configuration;
        if(configuration == null) {
            throw new IllegalStateException();
        }
        return configuration;
    }


}
