/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.validation;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.contract.validation.ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.edc.policy.model.Operator.LEQ;
import static org.eclipse.edc.policy.model.Operator.LT;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.mock;

@ComponentTest
class ContractExpiryCheckFunctionEvaluationTest {

    private static final Instant NOW = now();
    private final ContractExpiryCheckFunction function = new ContractExpiryCheckFunction();
    private final RuleBindingRegistry bindingRegistry = new RuleBindingRegistryImpl();
    private PolicyEngine policyEngine;

    @BeforeEach
    void setup() {
        // bind/register rule to evaluate contract expiry
        bindingRegistry.bind("USE", TRANSFER_SCOPE);
        bindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, TRANSFER_SCOPE);
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry));
        policyEngine.registerFunction(TRANSFER_SCOPE, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY, function);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidTimeProvider.class)
    void evaluate_fixed_isValid(Operator startOp, Instant start, Operator endOp, Instant end) {
        var policy = createInForcePolicy(start, startOp, end, endOp);
        var contextInfo = new HashMap<Class<?>, Object>();
        contextInfo.put(ContractAgreement.class, createAgreement("test-agreement"));

        contextInfo.put(Instant.class, NOW);
        assertThat(policyEngine.evaluate(TRANSFER_SCOPE, policy, mock(ParticipantAgent.class), contextInfo)).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidTimeProvider.class)
    void evaluate_fixed_isInvalid(Operator startOp, Instant start, Operator endOp, Instant end) {
        var policy = createInForcePolicy(start, startOp, end, endOp);
        var contextInfo = new HashMap<Class<?>, Object>();
        contextInfo.put(ContractAgreement.class, createAgreement("test-agreement"));

        contextInfo.put(Instant.class, NOW);
        assertThat(policyEngine.evaluate(TRANSFER_SCOPE, policy, mock(ParticipantAgent.class), contextInfo))
                .isFailed()
                .detail().contains(CONTRACT_EXPIRY_EVALUATION_KEY);
    }

    private Policy createInForcePolicy(Instant startDate, Operator operatorStart, Instant endDate, Operator operatorEnd) {
        var fixedInForceTimeConstraint = AndConstraint.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(CONTRACT_EXPIRY_EVALUATION_KEY))
                        .operator(operatorStart)
                        .rightExpression(new LiteralExpression(startDate.toString()))
                        .build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(CONTRACT_EXPIRY_EVALUATION_KEY))
                        .operator(operatorEnd)
                        .rightExpression(new LiteralExpression(endDate.toString()))
                        .build())
                .build();
        var permission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .constraint(fixedInForceTimeConstraint).build();

        return Policy.Builder.newInstance()
                .permission(permission)
                .build();
    }

    private ContractAgreement createAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private static class ValidTimeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW, LEQ, NOW.plus(ofDays(1))),
                    of(GEQ, NOW.minus(ofDays(1)), LEQ, NOW),
                    of(GT, NOW.minus(ofSeconds(1)), LT, NOW.plusSeconds(1L)),
                    of(EQ, NOW, LT, NOW.plusSeconds(1)),
                    of(GEQ, NOW.minusSeconds(1), EQ, NOW),
                    of(NEQ, NOW.minusSeconds(4), LT, NOW.plusSeconds(10))
            );
        }
    }

    private static class InvalidTimeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    of(NEQ, NOW, LEQ, NOW.plusSeconds(1)), //lower bound violation
                    of(GEQ, NOW, NEQ, NOW), // upper bound violation
                    of(GEQ, NOW.plusSeconds(1), LEQ, NOW.plusSeconds(10)), //NOW is before start
                    of(GEQ, NOW.minusSeconds(30), LEQ, NOW.minusSeconds(10)), //NOW is after  end
                    of(GT, NOW, LEQ, NOW.plusSeconds(40)), // lower bound violation, NOW is exactly on start
                    of(GT, NOW.minusSeconds(10), LT, NOW), // upper bound violation, NOW is exactly on end
                    of(NEQ, NOW, LEQ, NOW.plusSeconds(30)) //start cannot be NOW, but it is
            );
        }
    }
}