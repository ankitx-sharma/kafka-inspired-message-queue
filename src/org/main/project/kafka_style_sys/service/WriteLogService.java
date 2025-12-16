package org.main.project.kafka_style_sys.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

public class WriteLogService {

	private FileChannel channel;
	
	private String fileName;
	
	private AtomicInteger messageNo = new AtomicInteger(0);
	
	private AtomicInteger fileNo = new AtomicInteger(0);

	public void appendMessageToLog(String message) throws IOException{
		if(channel == null) {
			openWriteChannel();
		}
		
		byte[] data = message.getBytes(StandardCharsets.UTF_8);
		ByteBuffer lenBuff = ByteBuffer.allocate(Integer.BYTES);
		lenBuff.putInt(data.length);
		lenBuff.flip();
		
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		
		if(messageNo.get() == 10) {
			channel.close();
			fileNo.getAndIncrement();
			openWriteChannel();
			messageNo.set(0);
		}
		
		writeFully(channel, lenBuff);
		writeFully(channel, dataBuffer);
		
		messageNo.incrementAndGet();
	}
	
	private void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException{
		while(buffer.hasRemaining()) {
			channel.write(buffer);
		}
	}
	
	private void openWriteChannel() throws IOException{
		fileName = "Logdb-"+fileNo.get()+".data";
		
		if(!Files.exists(Path.of(fileName))) { 
			Files.createFile(Path.of(fileName));
		}
		
		this.channel = FileChannel.open(
				Path.of(fileName), 
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
	}
}
