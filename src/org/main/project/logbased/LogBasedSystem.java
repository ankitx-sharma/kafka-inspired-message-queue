package org.main.project.logbased;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LogBasedSystem implements a simple append-only, disk-backed log for task or message persistence.
 *
 * <p>The system writes incoming messages sequentially to log segment files on disk using
 * {@link FileChannel} in append mode. Each message is assigned a logical message number
 * and its corresponding byte offset within the log file is tracked in an in-memory index.</p>
 *
 * <p>Once a fixed number of messages is reached per log segment, the current index is flushed
 * to disk via {@link IndexMapService}, the active log file is closed, and a new log segment
 * is created. This design enables durability, crash recovery, and message replay in a
 * Kafka-inspired, log-based architecture.</p>
 *
 * <p>The log is append-only and never overwritten. Index files provide a mapping between
 * message numbers and their byte offsets, allowing efficient lookup and replay of messages
 * from disk.</p>
 *
 * <p><b>Note:</b> This class focuses on persistence and log segmentation. Task execution and
 * consumer logic are intentionally decoupled.</p>
 */
public class LogBasedSystem {
	/** Service responsible for persisting in-memory index maps to disk. */
	private IndexMapService indexService = new IndexMapService();
	
	/** In-memory index mapping message number → byte offset in the current log file. */
	private Map<Integer, Long> index = new ConcurrentHashMap<>(10);
	
	/** FileChannel used for append-only writes to the active log segment. */
	private FileChannel channel;
	
	/** Name of the currently active log file. */
	private String fileName;
	
	/** Logical message counter within the current log segment. */
	private AtomicInteger messageNo = new AtomicInteger(0);
	
	/** Log segment counter used to generate unique log file names. */
	private AtomicInteger fileNo = new AtomicInteger(0);
	
	/**
	 * Creates a new LogBasedSystem instance and initializes the first log segment.
	 *
	 * <p>If the initial log file does not exist, it is created. A {@link FileChannel}
	 * is then opened in append mode to allow sequential writes.</p>
	 *
	 * @throws IOException if the log file cannot be created or opened
	 */
	public LogBasedSystem() throws IOException{
		fileName = "Logdb-"+fileNo.get()+".data";
		
		if(!Files.exists(Path.of(fileName))) {
			createNewFile();
		}
		openChannel();
	}
	
	/**
	 * Processes a message after it has been retrieved from the log.
	 *
	 * <p>This method currently acts as a placeholder and simply prints the message.
	 * In a production system, this would contain business logic or task execution.</p>
	 *
	 * @param message the message to be processed
	 */
	public void processTheMessage(String message) {
		System.out.println("Message: "+message);
	}
	
	/**
	 * Appends a message to the active log segment and updates the in-memory index.
	 *
	 * <p>The message is written sequentially to disk using an append-only
	 * {@link FileChannel}. Before writing, the current file position is recorded
	 * and stored in the index map as the byte offset for the message.</p>
	 *
	 * <p>When the configured message limit per segment is reached, the following
	 * actions are performed atomically:
	 * <ul>
	 *   <li>The current index is flushed to disk</li>
	 *   <li>The active log file is closed</li>
	 *   <li>A new log segment is created</li>
	 *   <li>The message counter is reset</li>
	 * </ul>
	 * </p>
	 *
	 * @param message the message to append to the log
	 * @throws IOException if writing to disk fails
	 */
	public void appendMessageToLog(String message) throws IOException{
		byte[] byteMessage = message.getBytes();
		ByteBuffer buff = ByteBuffer.wrap(byteMessage);
		
		if(messageNo.get() == 10) {
			indexService.writeIndexToDisk(index, 10, fileNo.get());
			channel.close();
			fileNo.getAndIncrement();
			createNewFile();
			openChannel();
			messageNo.set(0);
		}
		
		index.put(messageNo.getAndIncrement(), channel.position());
		channel.write(buff);
	}
	
	/**
	 * Gracefully shuts down the log system.
	 *
	 * <p>Flushes the remaining in-memory index to disk and closes the active
	 * {@link FileChannel}. This method should be called during controlled
	 * shutdown to ensure index consistency.</p>
	 *
	 * @throws IOException if the index cannot be written or the channel cannot be closed
	 */
	public void endTheSystem() throws IOException{
		indexService.writeIndexToDisk(index, messageNo.get(), fileNo.get());
		channel.close();
	}
	
	/**
	 * Creates a new log segment file based on the current segment number.
	 *
	 * <p>The file name follows the pattern {@code Logdb-<segment>.data}.</p>
	 *
	 * @throws IOException if the file cannot be created
	 */
	public void createNewFile() throws IOException{
		fileName = "Logdb-"+fileNo.get()+".data";
		Files.createFile(Path.of(fileName));
	}
	
	/**
	 * Opens a {@link FileChannel} for the current log segment in append mode.
	 *
	 * <p>The channel is configured to create the file if it does not exist
	 * and to always append new data to the end of the file.</p>
	 *
	 * @throws IOException if the channel cannot be opened
	 */
	public void openChannel() throws IOException{
		this.channel = FileChannel.open(
				Path.of(fileName), 
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
	}
}
