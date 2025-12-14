package org.main.project;

import java.io.IOException;
import java.util.Map;

import org.main.project.logbased.LogBasedSystem;

public class MainLogClass {
	public static void main(String[] args) throws IOException{
		LogBasedSystem log = new LogBasedSystem();
		
		for(int i=0;i<25;i++) {
			log.appendMessageToLog("Message: "+i);
		}
		
		log.endTheSystem();
		log.displayIndex();
		
		Map<Integer, Long> index = log.readIndexFromDisk(); 
		
		for(Map.Entry<Integer, Long> entry: index.entrySet()) {
			System.out.println("Key: "+entry.getKey());
			System.out.println("Value: "+entry.getValue());
			System.out.println();
		}
	}
}
