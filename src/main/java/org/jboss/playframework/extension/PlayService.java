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

package org.jboss.playframework.extension;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import play.Play;

/**
 * @author Emanuel Muckenhuber
 */
class PlayService implements Service<Play> {

    final Play play = new Play();

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        Play.standalonePlayServer = false;
        Play.forceProd = true;
        try {
            Play.start();
        } catch (Exception e) {
            throw new StartException("failed to start play framework", e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        Play.stop();
    }

    @Override
    public synchronized Play getValue() throws IllegalStateException, IllegalArgumentException {
        final Play play = this.play;
        return play;
    }

}
