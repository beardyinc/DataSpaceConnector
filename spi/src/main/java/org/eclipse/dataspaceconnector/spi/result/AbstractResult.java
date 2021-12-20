package org.eclipse.dataspaceconnector.spi.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public abstract class AbstractResult<T, F extends Failure> {

    private final T content;
    private final F failure;

    protected AbstractResult(T content, F failure) {
        this.content = content;
        this.failure = failure;
    }

    @NotNull
    public T getContent() {
        Objects.requireNonNull(content);
        return content;
    }

    public F getFailure() {
        return failure;
    }

    @JsonIgnore
    public List<String> getFailureMessages() {
        Objects.requireNonNull(failure);
        return failure.getMessages();
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean failed() {
        return !succeeded();
    }
}
