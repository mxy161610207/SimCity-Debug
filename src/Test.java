import java.io.*;   

import sun.audio.*;

public class Test {
	public static void main(String[] args) {
        try {  
            // 1.wav �ļ�����java project ����  
            AudioStream as = new AudioStream(new FileInputStream("res/crash.wav"));  
            AudioPlayer.player.start(as);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
	}
}
