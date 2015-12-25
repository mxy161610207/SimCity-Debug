import java.io.*;   

import sun.audio.*;

public class Test {
	public static void main(String[] args) {
        try {  
            AudioStream as = new AudioStream(new FileInputStream("res/oh_no.wav"));  
            AudioPlayer.player.start(as);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
	}
}
