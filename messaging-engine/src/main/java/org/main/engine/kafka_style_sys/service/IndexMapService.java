package org.main.engine.kafka_style_sys.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IndexMapService is responsible for persisting and restoring index metadata
 * for log segments in a log-based storage system.
 *
 * <p>The index maps logical message numbers (Integer) to their corresponding
 * byte offsets (Long) within a log segment file. This mapping enables efficient
 * lookup, replay, and recovery of messages stored on disk.</p>
 *
 * <p>Index files are stored separately from log files and are written in a
 * compact binary format using {@link DataOutputStream}. Each index file
 * corresponds to a single log segment.</p>
 *
 * <p>This service abstracts index persistence logic away from the core log
 * writer, keeping responsibilities clearly separated.</p>
 */
public class IndexMapService {
	/** Tracks the currently active log segment number for index recovery. */
	private AtomicInteger activeFileNo = new AtomicInteger(0);
	
	/** In-memory representation of the active index loaded from disk. */
	private Map<Integer, Long> activeIndex = new ConcurrentHashMap<>(10);
	
	/**
	 * Writes an in-memory index map to disk in a binary format.
	 *
	 * <p>The index file stores a fixed number of entries, beginning with the total
	 * entry count followed by pairs of message numbers and byte offsets. Each index
	 * file is associated with a specific log segment.</p>
	 *
	 * <p>The file naming convention follows the pattern {@code index-<segment>.data}.</p>
	 *
	 * @param index the in-memory index mapping message numbers to byte offsets
	 * @param writeSize the number of index entries to persist
	 * @param fileNo the log segment number associated with this index
	 */
	public void writeIndexToDisk(Map<Integer, Long> index, Integer writeSize, Integer fileNo) {
		String indexFileName = "index-"+fileNo+".data";
		try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFileName)))){
			out.writeInt(writeSize);
			
			for(Map.Entry<Integer, Long> entry: index.entrySet()) {
				out.writeInt(entry.getKey());
				out.writeLong(entry.getValue());
				if((--writeSize) == 0) {
					break;
				}
			}
		}catch(FileNotFoundException e) {
			System.out.print("File not found");
		}catch(IOException e) {
			System.out.println("IO Exception occurred");
		}
	}
	
	/**
	 * Reads an index file from disk and reconstructs the in-memory index map.
	 *
	 * <p>The method reads the index file associated with the currently active
	 * log segment and populates an in-memory map with message number to byte
	 * offset mappings.</p>
	 *
	 * <p>This method is typically used during system startup or recovery to
	 * restore index state after a crash or restart.</p>
	 *
	 * @return a map containing message number to byte offset mappings read from disk
	 */
	public Map<Integer, Long> readIndexFromDisk() {
		String indexFileName = "index-"+activeFileNo.get()+".data";
		try(DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(indexFileName)))){
			int size = in.readInt();
			for(int i=0; i<size; i++) {
				int key = in.readInt();
				long value = in.readLong();
				activeIndex.put(key, value);
			}
		}catch(FileNotFoundException e) {
			System.out.print("File not found");
		}catch(IOException e) {
			System.out.println("IO Exception occurred");
		}
		
		return activeIndex;
	}
}
