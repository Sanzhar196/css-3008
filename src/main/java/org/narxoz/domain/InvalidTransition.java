package org.narxoz.domain;

public class InvalidTransition extends IllegalStateException {
    public InvalidTransition(String message) {
        super(message);
    }
}
