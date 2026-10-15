package com.edusuite.visitors.visit;

/** Lifecycle states of a visit; see docs/spec.md §4 for the allowed transitions. */
public enum VisitState {
	EXPECTED, AWAITING_HOST, APPROVED, DECLINED, TIMED_OUT, CHECKED_IN, CHECKED_OUT
}
