package org.opencv.samples.facedetect;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class Communication {
	
	
	private final String TAG = "Communication";
	
	public boolean makeConnection() {
		
		Socket myClient;
		DataInputStream input;
		
		try {
			
			myClient = new Socket( "http://franksevenhuysen.nl", 8080 );
			
			input = new DataInputStream( myClient.getInputStream() );
			
		}
		catch( IOException e ) {
			
			
		}
		

		return true;
	}

}
