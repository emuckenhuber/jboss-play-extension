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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.vfs.VirtualFile;

/**
 * Parsing processor.
 *
 * @author Emanuel Muckenhuber
 */
public class PlayApplicationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if(deploymentRoot.getChild("app").exists() && deploymentRoot.getChild("conf/application.conf").exists()) {
            final VirtualFile appConf = deploymentRoot.getChild("conf/application.conf");
            try {
                final Properties configuration = new Properties();
                final InputStream is = appConf.openStream();
                try {
                    configuration.load(is);
                } finally {
                    if(is != null) {
                        try {
                            is.close();
                        } catch(IOException e) {
                            // ignore
                        }
                    }
                }
                final PlayApplication app = new PlayApplication(deploymentRoot, configuration);
                deploymentUnit.putAttachment(PlayApplication.APPLICATION_KEY, app);
            } catch(IOException e) {
                throw new DeploymentUnitProcessingException("failed to process application.conf", e);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        //
    }

}
