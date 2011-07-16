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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The Play! eXtension.
 *
 * @author Emanuel Muckenhuber
 */
public final class PlayExtension implements Extension {

    /** {@inheritDoc} */
    public void initialize(final ExtensionContext context) {
        // Register the play subsystem
        final SubsystemRegistration subsystem = context.registerSubsystem(Constants.SUBSYTEM);
        subsystem.registerXMLElementWriter(SubsystemParser.INSTANCE);

        // Register the management sub resource
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(PlaySubsystemProviders.SUBSYSTEM);

        // Register the required subsystem handlers
        registration.registerOperationHandler(ADD, PlaySubsystemAdd.INSTANCE, PlaySubsystemProviders.SUBSYSTEM_ADD);
        registration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, EntryType.PRIVATE);

    }

    /** {@inheritDoc} */
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Constants.NAMESPACE, SubsystemParser.INSTANCE);
    }

    private static ModelNode createAddSubsystemOperation(final ModelNode subModel) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, Constants.SUBSYTEM);
        subsystem.get(PATH).set(subModel.get(PATH));
        subsystem.get(RELATIVE_TO).set(subModel.get(RELATIVE_TO));
        return subsystem;
    }

    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
        static final SubsystemParser INSTANCE = new SubsystemParser();

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Constants.NAMESPACE, false);

            final ModelNode model = context.getModelNode();

            writer.writeEmptyElement(Constants.FRAMEWORK_PATH);
            writer.writeAttribute(PATH, model.get(PATH).asString());
            if(model.hasDefined(RELATIVE_TO)) {
                writer.writeAttribute(RELATIVE_TO, model.get(RELATIVE_TO).asString());
            }

            writer.writeEndElement();
        }

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            ModelNode path = null;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case PLAY_1_0:
                        final Element element = Element.forName(reader.getLocalName());
                        switch(element) {
                            case FRAMEWORK_PATH:
                                path = parsePath(reader);
                                break;
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }
            if(path == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Element.FRAMEWORK_PATH));
            }
            list.add(createAddSubsystemOperation(path));
        }

        static ModelNode parsePath(XMLExtendedStreamReader reader) throws XMLStreamException {
            String path = null;
            String relativeTo = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH: {
                        path = value;
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(path == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PATH));
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            final ModelNode model = new ModelNode();
            model.get(PATH).set(path);
            if(relativeTo != null) model.get(RELATIVE_TO).set(relativeTo);
            return model;
        }

    }

    private static class SubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final SubsystemDescribeHandler INSTANCE = new SubsystemDescribeHandler();

        /** {@inheritDoc} */
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            context.getResult().add(createAddSubsystemOperation(resource.getModel()));
            context.completeStep();
        }

        /** {@inheritDoc} */
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
