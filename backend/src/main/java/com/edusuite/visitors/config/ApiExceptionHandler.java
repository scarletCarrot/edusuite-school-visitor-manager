package com.edusuite.visitors.config;

import java.util.NoSuchElementException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.edusuite.visitors.visit.VisitRuleException;

/** Maps failures to RFC 9457 problem responses (docs/spec.md §13); invalid input is handled by the base class. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler
	ProblemDetail ruleBroken(VisitRuleException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
	ProblemDetail concurrentChange(DataAccessException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Someone else changed this visit or card at the same moment; refresh and try again");
	}

	@ExceptionHandler
	ProblemDetail notFound(NoSuchElementException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail forbidden(AccessDeniedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
	}

}
