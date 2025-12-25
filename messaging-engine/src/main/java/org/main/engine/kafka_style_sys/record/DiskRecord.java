package org.main.engine.kafka_style_sys.record;

public record DiskRecord(long nextPos, String message) {}
