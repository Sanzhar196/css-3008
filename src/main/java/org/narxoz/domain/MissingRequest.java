package org.narxoz.domain;

public class MissingRequest extends RuntimeException {
    public MissingRequest(String id) {
        super("request not found");
    }
}
