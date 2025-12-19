package org.main.project.kafka_style_sys.service;

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

import org.main.project.kafka_style_sys.record.DiskRecord;

public class FileDiskQueue implements DiskQueue{
	private final Path path;
	private final ReentrantLock lock = new ReentrantLock(true);
	
	private FileChannel channel;
	
	private long readPos = 0L;
	private long commitPos = 0L;
	
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
	
	@Override
	public boolean isEmpty() throws IOException {
		lock.lock();
		try {
			return readPos >= channel.size();
		}finally {
			lock.unlock();
		}
	}
	
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
