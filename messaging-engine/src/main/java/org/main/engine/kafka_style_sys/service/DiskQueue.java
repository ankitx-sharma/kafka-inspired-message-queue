package org.main.engine.kafka_style_sys.service;

import java.io.IOException;

import org.main.engine.kafka_style_sys.record.DiskRecord;

/**
 * A simple disk-backed FIFO queue abstraction.
 *
 * <p>Implementations are responsible for persisting messages,
 * reading them in order, and acknowledging processed entries.
 */
public interface DiskQueue {
	/**
     * Appends a message to the end of the queue.
     *
     * @param msg message to persist
     * @throws IOException if the write fails
     */
	void append(String msg) throws IOException;
	
	/**
     * Reads the next available record from disk without removing it.
     *
     * <p>The returned record must be acknowledged using {@link #ack(long)}
     * after successful processing.
     *
     * @return the next {@link DiskRecord}, or {@code null} if the queue is empty
     * @throws IOException if reading fails or data is corrupt
     */
	DiskRecord poll() throws IOException;
	
	/**
     * Acknowledges that all data up to the given position
     * has been successfully processed.
     *
     * @param nextPos file position after the processed record
     * @throws IOException if acknowledgement fails
     */
	void ack(long nextPos) throws IOException;
	
	/**
     * Checks whether the queue has no unread data.
     *
     * @return {@code true} if no records are available
     * @throws IOException if the check fails
     */
	boolean isEmpty() throws IOException;
	
	 /**
     * Closes the queue and releases any underlying resources.
     *
     * @throws IOException if closing fails
     */
	void close() throws IOException;
}
