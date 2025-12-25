package org.main.engine.service;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

import org.main.engine.dto.DiskRecord;

/**
 * File-based implementation of {@link DiskQueue}.
 *
 * <p>Records are stored sequentially in a single file using the format:
 * <pre>
 * [4 bytes length][UTF-8 payload]
 * </pre>
 *
 * <p>This implementation is:
 * <ul>
 *   <li>Thread-safe (protected by a {@link ReentrantLock})</li>
 *   <li>Append-only</li>
 *   <li>FIFO ordered</li>
 * </ul>
 *
 * <p>Processed records are tracked using read and commit positions.
 */
public class FileDiskQueue implements DiskQueue{
	private final Path path;
	private final ReentrantLock lock = new ReentrantLock(true);
	
	private FileChannel channel;
	
	private long readPos = 0L;
	private long commitPos = 0L;
	
	 /**
     * Creates or opens a file-backed queue at the given file name.
     *
     * @param fileName queue file path
     * @throws IOException if the file cannot be created or opened
     */
	public FileDiskQueue(String fileName) throws IOException{
		this.path = Paths.get(fileName);
		open();
	}
	
	private void open() throws IOException{
		if(!Files.exists(path)) {
			Files.createFile(path);
		}
		
		this.channel = FileChannel.open(path, CREATE, READ, WRITE);
	}
	
	/**
     * Appends a message to the end of the queue file.
     *
     * <p>The message is written as length-prefixed UTF-8 data
     * and flushed to disk.
     *
     * @param msg message to append
     * @throws IOException if writing fails
     */
	@Override
	public void append(String msg) throws IOException {
		lock.lock();
		
		try {
			byte[] data = msg.getBytes(StandardCharsets.UTF_8);
			
			ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
			len.putInt(data.length);
			len.flip();
			
			ByteBuffer payload = ByteBuffer.wrap(data);
			
			channel.position(channel.size());
			writeFully(len);
			writeFully(payload);
			
			channel.force(false);
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Reads the next record from disk starting at the current read position.
     *
     * <p>If the end of file is reached, {@code null} is returned.
     * Corrupted or incomplete records cause an {@link IOException}.
     *
     * @return next {@link DiskRecord}, or {@code null} if no data is available
     * @throws IOException if reading fails or data is invalid
     */
	@Override
	public DiskRecord poll() throws IOException {
		lock.lock();
		try {
			if(readPos >= channel.size()) { //end of file reached
				return null;
			}
			
			ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
			int lenRead = readAtLeast(lenBuf, readPos);
			if(lenRead < Integer.BYTES) return null; // incorrect data so return null
			
			lenBuf.flip();
			int len = lenBuf.getInt();
			
			if(len < 0 || len > 10_000_000) {
				throw new IOException("Corrupt queue: invalid record length " + len);
			}
			
			long payloadPos = readPos + Integer.BYTES;
			
			ByteBuffer dataBuf = ByteBuffer.allocate(len);
			int dataRead = readAtLeast(dataBuf, payloadPos);
			if(dataRead < len) {
				throw new IOException("Corrupt queue: incomplete payload");
			}
			
			dataBuf.flip();
			byte[] data = new byte[len];
			dataBuf.get(data);
			
			long nextPos = payloadPos + len;
			readPos = nextPos;
			
			return new DiskRecord(nextPos, new String(data, StandardCharsets.UTF_8));
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Acknowledges processing progress by advancing the commit position.
     *
     * <p>Calling ack with an older position has no effect.
     *
     * @param nextPos file position after the processed record
     * @throws IOException if acknowledgement fails
     */
	@Override
	public void ack(long nextPos) throws IOException {
		lock.lock();
		try {
			if(nextPos <= commitPos) return;
			commitPos = nextPos;
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Checks whether all data in the file has been read.
     *
     * @return {@code true} if no unread records exist
     * @throws IOException if the check fails
     */
	@Override
	public boolean isEmpty() throws IOException {
		lock.lock();
		try {
			return readPos >= channel.size();
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Closes the file channel and deletes the underlying queue file.
     *
     * @throws IOException if closing or deletion fails
     */
	@Override
	public void close() throws IOException {
		lock.lock();
		try {
			if(channel!=null) channel.close();
			Files.delete(path);
		}finally {
			lock.unlock();
		}
	}
	
	private void writeFully(ByteBuffer buffer) throws IOException{
		while(buffer.hasRemaining()) {
			channel.write(buffer);
		}
	}
	
	private int readAtLeast(ByteBuffer buffer, long position) throws IOException{
		int total = 0;
		while(buffer.hasRemaining()) {
			int n = channel.read(buffer, position + total);
			if(n == -1) break;
			total += n;
		}
		return total;
	}
}
