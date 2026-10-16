package org.narxoz.domain;

public class DuplicateRequest extends RuntimeException {
    public DuplicateRequest(String businessKey) {
        super("duplicate request: " + businessKey);
    }
}
