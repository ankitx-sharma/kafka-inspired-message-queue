package org.main.project.logbased;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LogBasedSystem {

	private Map<Integer, Long> index = new ConcurrentHashMap<>(10);
	private Map<Integer, Long> activeIndex = new ConcurrentHashMap<>(10);
	private FileChannel channel;
	private String fileName;
	
	private AtomicInteger messageNo = new AtomicInteger(0);
	private AtomicInteger fileNo = new AtomicInteger(0);
	private AtomicInteger activeFileNo = new AtomicInteger(0);
	
	public LogBasedSystem() throws IOException{
		fileName = "Logdb-"+fileNo.get()+".data";
		
		if(!Files.exists(Path.of(fileName))) {
			createNewFile();
		}
		openChannel();
	}
	
	public void processTheMessage(String message) {
		System.out.println("Message: "+message);
	}
	
	public void appendMessageToLog(String message) throws IOException{
		byte[] byteMessage = message.getBytes();
		ByteBuffer buff = ByteBuffer.wrap(byteMessage);
		
		if(messageNo.get() == 10) {
			writeIndexToDisk(10);
			channel.close();
			fileNo.getAndIncrement();
			createNewFile();
			openChannel();
			messageNo.set(0);
		}
		
		index.put(messageNo.getAndIncrement(), channel.position());
		channel.write(buff);
	}
	
	public void writeIndexToDisk(Integer writeSize) {
		String indexFileName = "index-"+fileNo.get()+".data";
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
	
	public void endTheSystem() throws IOException{
		writeIndexToDisk(messageNo.get());
		channel.close();
	}
	
	public void createNewFile() throws IOException{
		fileName = "Logdb-"+fileNo.get()+".data";
		Files.createFile(Path.of(fileName));
	}
	
	public void openChannel() throws IOException{
		this.channel = FileChannel.open(
				Path.of(fileName), 
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
	}
	
	public void displayIndex() {
		for(Integer key: index.keySet()) {
			System.out.println(key+": "+index.get(key));
		}
	}
}
