package org.main.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.main.engine.dto.DiskRecord;

public class FileDiskQueueTest {
	
	@TempDir
	Path tempDir;
	
	@Test
	void append_and_poll_shouldBeFifo() throws Exception {
		Path file = tempDir.resolve("q.bin");
		FileDiskQueue q = new FileDiskQueue(file.toString());
		
		assertTrue(q.isEmpty());
		
		q.append("A");
		q.append("B");
		
		DiskRecord r1 = q.poll();
		assertNotNull(r1);
		assertEquals("A", r1.message());
		
		DiskRecord r2 = q.poll();
		assertNotNull(r2);
		assertEquals("B", r2.message());
		
		assertNull(q.poll());
		assertTrue(q.isEmpty());
		
		q.close();
	}
	
	@Test
	void poll_onEmpty_shouldReturnNull() throws Exception {
		Path file = tempDir.resolve("q.bin");
		FileDiskQueue q = new FileDiskQueue(file.toString());
		
		assertNull(q.poll());
		assertTrue(q.isEmpty());
		
		q.close();
	}
	
	@Test
	void poll_withCorruptLength_shouldThrowIOException() throws Exception {
		Path file = tempDir.resolve("q.bin");
		
		try(FileChannel ch = FileChannel.open(file, 
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			ByteBuffer len = ByteBuffer.allocate(4);
			len.putInt(-1);
			len.flip();
			ch.write(len);
		}
		
		FileDiskQueue q = new FileDiskQueue(file.toString());
		
		IOException ex = assertThrows(IOException.class, q::poll);
		assertTrue(ex.getMessage().contains("invalid record length"));
		
		q.close();
	}
}
