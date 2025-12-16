package org.main.project.kafka_style_sys.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ReadLogService {
	
	private FileChannel channel;
	
	private long readPosition = 0L;
	
	private Integer currentFileNo;
	
	private String fileName;
	
	private void openReadChannel(Integer fileNo) throws IOException{
		currentFileNo = fileNo;
		fileName = "Logdb-"+fileNo+".data";
		
		if(channel != null) {
			channel.close();
		}
		
		if(!Files.exists(Path.of(fileName))) { 
			throw new FileNotFoundException("File does not exist");
		}
		
		this.channel = FileChannel.open(Path.of(fileName), 
						StandardOpenOption.READ);
		this.readPosition = 0L;
	}
	
	public String readMessage(Integer fileNo) throws IOException{
		if(channel == null || !fileNo.equals(currentFileNo)) {
			openReadChannel(fileNo);
		}
		
		ByteBuffer lenBuff = ByteBuffer.allocate(Integer.BYTES);
		int readLenBytes = readAtLeast(lenBuff, readPosition);
		
		if(readLenBytes < Integer.BYTES) {
			try {
				channel.close();
				Files.delete(Path.of(fileName));
			}catch(FileNotFoundException ex) {
				return "eof";
			}
		}
		
		lenBuff.flip();
		int len = lenBuff.getInt();
		
		if(len < 0 || len > 10_000_000) {
			throw new IOException("Corrupt log: invalid record length "+len);
		}
		
		readPosition += Integer.BYTES;
		
		ByteBuffer dataBuff = ByteBuffer.allocate(len);
		int readDataBytes = readAtLeast(dataBuff, readPosition);
		
		if(readDataBytes < len) {
			throw new IOException("Corrupt log: incomplete record payload");
		}
		
		readPosition +=len;
		dataBuff.flip();
		
		byte[] data = new byte[len];
		dataBuff.get(data);
		
		return new String(data, StandardCharsets.UTF_8);
	}
	
	private int readAtLeast(ByteBuffer buffer, long position) throws IOException{
		int total = 0;
		while(buffer.hasRemaining()) {
			int data = channel.read(buffer, position+total);
				if(data == -1) {
					break;
				}
			total += data;
		}
		
		return total;
	}
}
