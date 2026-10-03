package org.narxoz.domain;

import java.util.List;
import java.util.Objects;

public class RequestPolicy implements Rule {
    private final List<Rule> rules;

    public RequestPolicy() {
        this(List.of(new TransitionRule(), new OnlyApprovedCanCancelRule()));
    }

    public RequestPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public void check(RequestStatus from, RequestStatus to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        rules.forEach(r -> r.check(from, to));
    }

    public RequestStatus move(RequestStatus from, RequestStatus to) {
        check(from, to);
        return to;
    }
}