import java.io.*;   

import sun.audio.*;

public class Test {
	public static void main(String[] args) {
        try {  
            // 1.wav 文件放在java project 下面  
            AudioStream as = new AudioStream(new FileInputStream("res/crash.wav"));  
            AudioPlayer.player.start(as);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
	}
}
