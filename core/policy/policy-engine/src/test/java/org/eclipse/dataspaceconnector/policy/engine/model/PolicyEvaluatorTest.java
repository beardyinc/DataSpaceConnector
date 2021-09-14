/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.engine.model;

import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluator;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.policy.engine.model.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEvaluatorTest {

    @Test
    void verifySimpleEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("foo", "foo");

        Duty duty = Duty.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().duty(duty).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionNotEqualEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("baz", "bar");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionNotEqualEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("baz", "bar");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("toResolve", (operator, value, p) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyDutyFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Duty duty = Duty.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().duty(duty).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().dutyFunction("toResolve", (operator, value, d) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().prohibitionFunction("toResolve", (operator, value, pr) -> !"foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }
}
