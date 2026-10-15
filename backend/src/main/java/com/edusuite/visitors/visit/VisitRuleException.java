package com.edusuite.visitors.visit;

/** A request that breaks a visit lifecycle rule; mapped to HTTP 409. */
public class VisitRuleException extends RuntimeException {

	public VisitRuleException(String message) {
		super(message);
	}

}
