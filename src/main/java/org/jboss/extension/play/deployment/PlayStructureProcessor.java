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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.extension.play.api.PlayFrameworkConfiguration;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * The structure processor.
 *
 * @author Emanuel Muckenhuber
 */
public class PlayStructureProcessor implements DeploymentUnitProcessor {

    static final VirtualFileFilter DEFAULT_LIB_VIRTUAL_FILE_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final PlayApplication application = deploymentUnit.getAttachment(PlayApplication.APPLICATION_KEY);
        if(application == null) {
            return;
        }
        final PlayFrameworkConfiguration configuration = deploymentUnit.getAttachment(PlayApplication.FRAMEWORK_KEY);
        if(configuration == null) {
            throw new DeploymentUnitProcessingException("framework not configured");
        }
        // Get the deployment root
        final ResourceRoot deploymentResourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentRoot = deploymentResourceRoot.getRoot();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpecification.setPrivateModule(true);
        // Don't index the root resource
        deploymentResourceRoot.putAttachment(Attachments.INDEX_RESOURCE_ROOT, false);

        final List<ResourceRoot> resources = new ArrayList<ResourceRoot>();
        try {

            final VirtualFile temp = deploymentRoot.getChild("temp"); // Virtual temp mountpoint

            // FIXME
            resources.add(createZipRoot(temp, new File(configuration.getFrameworkRoot(), "play-1.2.2.jar")));
            resources.add(createZipRoot(temp, new File(configuration.getFrameworkRoot(), "play-jboss-plugin.jar")));

            //
            resources.add(new ResourceRoot(deploymentRoot.getChild("conf"), new MountHandle(null)));

            //
            processLibs(temp, configuration.getFrameworkLib(), resources);
            processLibs(deploymentRoot.getChild("lib"), resources);

        } catch(IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        for(final ResourceRoot root : resources) {
            ModuleRootMarker.mark(root);
            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, root);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

    void processLibs(final VirtualFile lib, List<ResourceRoot> entries) throws IOException, DeploymentUnitProcessingException {
        if(lib.exists()) {
            final List<VirtualFile> archives = lib.getChildren(DEFAULT_LIB_VIRTUAL_FILE_FILTER);
            for(final VirtualFile archive : archives) {
                try {
                    final ResourceRoot root = createZipRoot(archive);
                    entries.add(root);
                } catch (IOException e) {
                    throw new DeploymentUnitProcessingException("failed to process " + archive, e);
                }
            }
        }
    }

    void processLibs(final VirtualFile temp, final File lib, List<ResourceRoot> entries) throws IOException, DeploymentUnitProcessingException {
        if(lib.exists()) {
            final File[] children = lib.listFiles();
            for(final File file : children) {
                if(! file.isDirectory() && file.getName().endsWith(".jar")) {
                    final ResourceRoot root = createZipRoot(temp, file);
                    entries.add(root);
                }
            }
        }
    }

    ResourceRoot createZipRoot(final VirtualFile deploymentTemp, final File file) throws IOException {
        final VirtualFile archive = deploymentTemp.getChild(file.getName());
        final Closeable closable = VFS.mountZip(file, archive, TempFileProviderService.provider());
        return new ResourceRoot(file.getName(), archive, new MountHandle(closable));
    }

    ResourceRoot createZipRoot(final VirtualFile archive) throws IOException {
        final Closeable closable = VFS.mountZip(archive, archive, TempFileProviderService.provider());
        return new ResourceRoot(archive.getName(), archive, new MountHandle(closable));
    }
}
