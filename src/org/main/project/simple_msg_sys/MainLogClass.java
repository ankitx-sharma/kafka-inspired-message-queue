package org.main.project.simple_msg_sys;

import java.io.IOException;
import java.util.Map;

import org.main.project.kafka_style_sys.service.IndexMapService;
import org.main.project.kafka_style_sys.service.WriteLogService;

public class MainLogClass {
	public static void main(String[] args) throws IOException{
		WriteLogService log = new WriteLogService();
		IndexMapService service = new IndexMapService();
		
		for(int i=0;i<25;i++) {
			log.appendMessageToLog("Message: "+i);
		}
		
		Map<Integer, Long> index = service.readIndexFromDisk(); 
		
		for(Map.Entry<Integer, Long> entry: index.entrySet()) {
			System.out.println("Key: "+entry.getKey());
			System.out.println("Value: "+entry.getValue());
			System.out.println();
		}
	}
}
