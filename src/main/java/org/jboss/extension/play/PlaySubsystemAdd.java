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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.extension.play.deployment.PlayApplicationProcessor;
import org.jboss.extension.play.deployment.PlayClassloadingDependenciesProcessor;
import org.jboss.extension.play.deployment.PlayInitializeProcessor;
import org.jboss.extension.play.deployment.PlayInstallProcessor;
import org.jboss.extension.play.deployment.PlayStructureProcessor;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation adding the Play! subsystem.
 *
 * @author Emanuel Muckenhuber
 */
class PlaySubsystemAdd implements OperationStepHandler {

    static final OperationStepHandler INSTANCE = new PlaySubsystemAdd();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        // Create the subsystem
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();
        // Persist the path information
        subModel.get(ModelDescriptionConstants.PATH).set(operation.require(ModelDescriptionConstants.PATH));
        if(operation.hasDefined(ModelDescriptionConstants.RELATIVE_TO)) {
            subModel.get(ModelDescriptionConstants.RELATIVE_TO).set(operation.get(ModelDescriptionConstants.RELATIVE_TO));
        }
        // Register the deployers
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(final DeploymentProcessorTarget target) {
                // FIXME the priority numbers and structure/mounting processors won't work
                target.addDeploymentProcessor(Phase.STRUCTURE, 0x002, new PlayInitializeProcessor());
                target.addDeploymentProcessor(Phase.PARSE, 0x050, new PlayApplicationProcessor());
                target.addDeploymentProcessor(Phase.PARSE, 0x051, new PlayStructureProcessor());
                target.addDeploymentProcessor(Phase.DEPENDENCIES, 0x2000, new PlayClassloadingDependenciesProcessor());
                target.addDeploymentProcessor(Phase.INSTALL, 0x050, new PlayInstallProcessor());
            }
        }, Stage.RUNTIME);
        // Start the configuration service
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final ServiceTarget serviceTarget = context.getServiceTarget();
                if(subModel.hasDefined(ModelDescriptionConstants.RELATIVE_TO)) {
                    RelativePathService.addService(PlayServices.PLAY_PATH, subModel, serviceTarget);
                } else {
                    AbsolutePathService.addService(PlayServices.PLAY_PATH, subModel, serviceTarget);
                }
                final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                final PlayFrameworkService service = new PlayFrameworkService();
                serviceTarget.addService(PlayServices.PLAY, service)
                    .addDependency(PlayServices.PLAY_PATH, String.class, service.path)
                    .addListener(verificationHandler)
                    .install();

                context.addStep(verificationHandler, Stage.VERIFY);

                if(context.completeStep() == ResultAction.ROLLBACK) {
                    context.removeService(PlayServices.PLAY);
                    context.removeService(PlayServices.PLAY_PATH);
                }

            }
        }, Stage.RUNTIME);

        context.completeStep();
    }
}
