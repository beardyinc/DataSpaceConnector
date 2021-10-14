/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQueryResponse;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class ContractOfferServiceImplTest {

    private ContractOfferFramework contractOfferFramework;
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        contractOfferFramework = mock(ContractOfferFramework.class);
        assetIndex = mock(AssetIndex.class);

        contractOfferService = new ContractOfferServiceImpl(contractOfferFramework, assetIndex);
    }

    @Test
    void testConstructorNullParametersThrowingIllegalArgumentException() {
        replay(contractOfferFramework, assetIndex);

        // just eval all constructor parameters are mandatory and lead to NPE
        assertThatThrownBy(() -> new ContractOfferServiceImpl(contractOfferFramework, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, null))
                .isInstanceOf(NullPointerException.class);

        verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningNullResultsEmptyStream() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        // expect
        expect(contractOfferFramework.queryTemplates(isA(ContractOfferFrameworkQuery.class)))
                .andReturn(null);

        replay(contractOfferFramework, assetIndex);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningEmptySelectorExpressionResultsEmptyStream() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        ContractOfferTemplate contractOfferTemplate = mock(ContractOfferTemplate.class);

        // expect
        expect(contractOfferFramework.queryTemplates(isA(ContractOfferFrameworkQuery.class)))
                .andReturn(Stream.of(contractOfferTemplate));
        expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(null);

        replay(contractOfferFramework, assetIndex, contractOfferTemplate);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        verify(contractOfferFramework, assetIndex, contractOfferTemplate);
    }

    @Test
    void testFullFlow() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        ContractOfferTemplate contractOfferTemplate = mock(ContractOfferTemplate.class);
        AssetSelectorExpression assetSelectorExpression = AssetSelectorExpression.Builder.newInstance().build();
        Asset asset = mock(Asset.class);
        Stream<Asset> assetStream = Stream.of(asset);
        ContractOffer contractOffer = mock(ContractOffer.class);

        // expect
        expect(contractOfferFramework.queryTemplates(
                isA(ContractOfferFrameworkQuery.class))).andReturn(Stream.of(contractOfferTemplate));
        expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(assetSelectorExpression);
        expect(assetIndex.queryAssets(assetSelectorExpression))
                .andReturn(assetStream);
        expect(contractOfferTemplate.getTemplatedOffers(assetStream))
                .andReturn(Stream.of(contractOffer));

        replay(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).containsExactly(contractOffer);

        verify(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);
    }
}