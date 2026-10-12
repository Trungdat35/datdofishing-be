package com.datdofishing.common.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** Vỏ chung của mọi sự kiện Kafka: id để chống xử lý trùng, loại sự kiện và dữ liệu. */
public record EventEnvelope(String eventId, String type, Instant occurredAt, JsonNode payload) {
}
