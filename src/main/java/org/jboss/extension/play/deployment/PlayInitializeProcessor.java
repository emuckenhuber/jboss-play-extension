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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountExplodedMarker;
import org.jboss.extension.play.PlayServices;

/**
 * Mount .play applications exploded.
 *
 * @author Emanuel Muckenhuber
 */
public class PlayInitializeProcessor implements DeploymentUnitProcessor {

    static final String PLAY_EXTENTION = ".play";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST)) {
            return;
        }
        if(deploymentUnit.getName().toLowerCase().endsWith(PLAY_EXTENTION)) {
            MountExplodedMarker.setMountExploded(deploymentUnit);
            phaseContext.addDeploymentDependency(PlayServices.PLAY, PlayApplication.FRAMEWORK_KEY);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        //
    }

}
