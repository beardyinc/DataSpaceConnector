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
package org.eclipse.dataspaceconnector.dataloading;


import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to facilitate validation and ingestion of objects into a backing data store (i.e. the {@link DataSink}).
 *
 * @param <T> The type of objects that are to be ingested.
 */
public class DataLoader<T> {
    private Collection<Function<T, ValidationResult>> validationPredicates;
    private DataSink<T> sink;

    protected DataLoader() {
        validationPredicates = new ArrayList<>();
    }

    /**
     * Inserts one item into the backing store if all validations pass.
     *
     * @param item The item to insert.
     * @throws ValidationException when one or more validations fail.
     */
    public void insert(T item) {
        // see that the item satisfies all predicates
        var failedValidations = validate(item).filter(ValidationResult::isInvalid)
                .collect(Collectors.toUnmodifiableList());

        // throw exception if item does not pass all validations
        if (!failedValidations.isEmpty()) {
            throw new ValidationException(failedValidations.stream().map(ValidationResult::getError).collect(Collectors.joining("; ")));
        }

        sink.accept(item);
    }

    /**
     * Accepts a collection of items into the backing store if they all pass validation. If even a single item fails validation, the
     * entire collection is rejected with a {@link ValidationException}.
     * <p>
     * Note that this does NOT implement transactional semantics in the database-sense. This means that if all items pass validation,
     * they are inserted one by one.
     *
     * @param items a Collection of items
     * @throws ValidationException when on or more items fail validation
     */
    public void insertAll(Collection<T> items) {

        var allValidationResults = items.stream().flatMap(this::validate);

        var errorMessages = allValidationResults.filter(ValidationResult::isInvalid).map(ValidationResult::getError).collect(Collectors.toList());

        if (!errorMessages.isEmpty()) {
            throw new ValidationException(String.join("; ", errorMessages));
        }

        items.forEach(sink::accept);
    }

    private Stream<ValidationResult> validate(T item) {
        return validationPredicates.stream().map(vr -> vr.apply(item));
    }

    public static final class Builder<T> {
        private final DataLoader<T> loader;

        private Builder() {
            loader = new DataLoader<>();
        }

        public static <T> Builder<T> newInstance() {
            return new Builder<>();
        }

        public Builder<T> sink(DataSink<T> sink) {
            loader.sink = sink;
            return this;
        }

        public Builder<T> andPredicate(Function<T, ValidationResult> predicate) {
            loader.validationPredicates.add(predicate);
            return this;
        }

        public Builder<T> predicates(Collection<Function<T, ValidationResult>> predicates) {
            loader.validationPredicates = predicates;
            return this;
        }

        public DataLoader<T> build() {
            return loader;
        }
    }
}


