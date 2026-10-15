package com.edusuite.visitors.visit;

import java.time.Instant;
import java.time.LocalDate;

/** The JSON shape of a visit (docs/spec.md §13). */
public record VisitView(Long id, VisitType type, VisitState state, String visitorName, String visitorPhone,
		String purpose, Long hostId, String hostName, LocalDate visitDate, Instant arrivedAt, Instant checkedInAt,
		Instant checkedOutAt, String passNumber, boolean overstayed) {

	static VisitView of(Visit visit, boolean overstayed) {
		return new VisitView(visit.getId(), visit.getType(), visit.getState(), visit.getVisitorName(),
				visit.getVisitorPhone(), visit.getPurpose(), visit.getHost().getId(), visit.getHost().getFullName(),
				visit.getVisitDate(), visit.getArrivedAt(), visit.getCheckedInAt(), visit.getCheckedOutAt(),
				visit.getPassNumber(), overstayed);
	}

}
