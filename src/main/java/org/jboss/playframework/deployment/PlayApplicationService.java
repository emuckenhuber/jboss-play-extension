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

package org.jboss.playframework.deployment;

import java.io.IOException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import play.Play;

/**
 * @author Emanuel Muckenhuber
 */
class PlayApplicationService implements Service<PlayApplication> {

    private final String app_id = "myapp"; // TODO allow multiple apps per server.
    private final PlayApplication application;
    private final InjectedValue<Play> framework = new InjectedValue<Play>();

    PlayApplicationService(final PlayApplication application) {
        this.application = application;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final Play play = framework.getValue();
        Play.ctxPath = "?";
        try {
            Play.init(application.getApplicationRoot().getPhysicalFile(), app_id);
        } catch (IOException e) {
            //
        }
    }

    @Override
    public void stop(StopContext context) {
        // weird...
    }

    @Override
    public PlayApplication getValue() throws IllegalStateException, IllegalArgumentException {
        final PlayApplication application = this.application;
        return application;
    }

}
