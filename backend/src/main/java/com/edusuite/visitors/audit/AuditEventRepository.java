package com.edusuite.visitors.audit;

import java.util.List;

import org.springframework.data.repository.Repository;

/** Insert and read only: no update or delete methods are exposed. */
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

	AuditEvent save(AuditEvent event);

	List<AuditEvent> findTop200ByOrderByIdDesc();

}
