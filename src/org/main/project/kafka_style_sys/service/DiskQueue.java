package org.main.project.kafka_style_sys.service;

import java.io.IOException;

import org.main.project.kafka_style_sys.record.DiskRecord;

public interface DiskQueue {
	void append(String msg) throws IOException;
	DiskRecord poll() throws IOException;
	void ack(long nextPos) throws IOException;
	boolean isEmpty() throws IOException;
	void close() throws IOException;
}
