package com.edusuite.visitors;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Business rules that the school may tune; see docs/spec.md §13. */
@ConfigurationProperties("visitors")
public record VisitorProperties(ZoneId zone, Duration hostResponseTimeout, Duration maxStay) {
}
