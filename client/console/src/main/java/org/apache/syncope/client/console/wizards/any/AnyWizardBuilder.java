/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.console.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.console.layout.AnyForm;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AnyWizardBuilder<A extends AnyTO> extends AjaxWizardBuilder<AnyWrapper<A>> {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final List<String> anyTypeClasses;

    protected AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo;

    /**
     * Construct.
     *
     * @param anyTO any
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final A anyTO,
            final List<String> anyTypeClasses,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo,
            final PageReference pageRef) {

        super(new AnyWrapper<>(anyTO), pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    /**
     * Construct.
     *
     * @param wrapper any wrapper
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final AnyWrapper<A> wrapper,
            final List<String> anyTypeClasses,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo,
            final PageReference pageRef) {

        super(wrapper, pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyWrapper<A> modelObject, final WizardModel wizardModel) {
        // optional details panel step
        addOptionalDetailsPanel(modelObject).ifPresent(wizardModel::add);

        if ((this instanceof GroupWizardBuilder)
                && (modelObject.getInnerObject() instanceof GroupTO)
                && (formLayoutInfo instanceof GroupFormLayoutInfo)) {

            GroupFormLayoutInfo groupFormLayoutInfo = GroupFormLayoutInfo.class.cast(formLayoutInfo);
            if (groupFormLayoutInfo.isOwnership()) {
                wizardModel.add(new Ownership(GroupWrapper.class.cast(modelObject), pageRef));
            }
            if (groupFormLayoutInfo.isDynamicMemberships()) {
                wizardModel.add(new DynamicMemberships(GroupWrapper.class.cast(modelObject)));
            }
        }

        if (formLayoutInfo.isAuxClasses()) {
            wizardModel.add(new AuxClasses(modelObject, anyTypeClasses));
        }

        if (formLayoutInfo.isGroups()) {
            wizardModel.add(new Groups(modelObject, mode == AjaxWizard.Mode.TEMPLATE));
        }

        // attributes panel steps
        if (formLayoutInfo.isPlainAttrs()) {
            wizardModel.add(new PlainAttrs(
                    modelObject,
                    null,
                    mode,
                    anyTypeClasses,
                    formLayoutInfo.getWhichPlainAttrs()) {

                private static final long serialVersionUID = 8167894751609598306L;

                @Override
                public PageReference getPageReference() {
                    return pageRef;
                }

            });
        }
        if (formLayoutInfo.isDerAttrs() && mode != AjaxWizard.Mode.TEMPLATE) {
            wizardModel.add(new DerAttrs(
                    modelObject, anyTypeClasses, formLayoutInfo.getWhichDerAttrs()));
        }
        if (formLayoutInfo.isVirAttrs()) {
            wizardModel.add(new VirAttrs(
                    modelObject, mode, anyTypeClasses, formLayoutInfo.getWhichVirAttrs()));
        }

        // role panel step (just available for users)
        if ((this instanceof UserWizardBuilder)
                && (modelObject.getInnerObject() instanceof UserTO)
                && (formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRoles()) {

            wizardModel.add(new Roles(modelObject));
        }

        // relationship panel step (available for users and any objects)
        if (((formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())
                || ((formLayoutInfo instanceof AnyObjectFormLayoutInfo)
                && AnyObjectFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())) {

            wizardModel.add(new Relationships(modelObject, pageRef));
        }

        // resource panel step
        if (formLayoutInfo.isResources()) {
            wizardModel.add(new Resources(modelObject));
        }

        return wizardModel;
    }

    protected Optional<Details<A>> addOptionalDetailsPanel(final AnyWrapper<A> modelObject) {
        if (modelObject.getInnerObject().getKey() == null) {
            return Optional.empty();
        } else {
            return Optional.of(new Details<>(modelObject, mode == AjaxWizard.Mode.TEMPLATE, true, pageRef));
        }
    }

    protected void fixPlainAndVirAttrs(final AnyTO updated, final AnyTO original) {
        // re-add to the updated object any missing plain or virtual attribute (compared to original): this to cope with
        // form layout, which might have not included some plain or virtual attributes
        for (AttrTO plainAttrTO : original.getPlainAttrs()) {
            if (!updated.getPlainAttr(plainAttrTO.getSchema()).isPresent()) {
                updated.getPlainAttrs().add(plainAttrTO);
            }
        }
        for (AttrTO virAttrTO : original.getVirAttrs()) {
            if (!updated.getVirAttr(virAttrTO.getSchema()).isPresent()) {
                updated.getVirAttrs().add(virAttrTO);
            }
        }

        if (updated instanceof GroupableRelatableTO && original instanceof GroupableRelatableTO) {
            GroupableRelatableTO.class.cast(original).getMemberships().forEach(oMemb -> {
                GroupableRelatableTO.class.cast(updated).getMembership(oMemb.getGroupKey()).ifPresent(uMemb -> {
                    oMemb.getPlainAttrs().stream().
                            filter(attr -> !uMemb.getPlainAttr(attr.getSchema()).isPresent()).
                            forEach(attr -> uMemb.getPlainAttrs().add(attr));
                    oMemb.getVirAttrs().stream().
                            filter(attr -> !uMemb.getVirAttr(attr.getSchema()).isPresent()).
                            forEach(attr -> uMemb.getVirAttrs().add(attr));
                });
            });
        }

        // remove from the updated object any plain or virtual attribute without values, thus triggering for removal in
        // the generated patch
        updated.getPlainAttrs().removeIf(attr -> attr.getValues().isEmpty());
        updated.getVirAttrs().removeIf(attr -> attr.getValues().isEmpty());
        if (updated instanceof GroupableRelatableTO) {
            GroupableRelatableTO.class.cast(updated).getMemberships().forEach(memb -> {
                memb.getPlainAttrs().removeIf(attr -> attr.getValues().isEmpty());
                memb.getVirAttrs().removeIf(attr -> attr.getValues().isEmpty());
            });
        }
    }

}
