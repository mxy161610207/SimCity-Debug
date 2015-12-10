/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.dataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 *
 * @author bingying
 * 将00.txt等转换为add/delete的change序列
 * status非0匹配（倒数第二位）
 * 先delete再add（两条数据相差时间大于生存时间，应该先删掉过期数据）
 */
public class Transform {
    private static long convert(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss:SSS" );
        Date date = sdf.parse(time);
        return date.getTime();
    }
    public static void trans() throws Exception {
        LinkedList<String> list = new LinkedList<String>();
        LinkedList<String> list1 = new LinkedList<String>();
        LinkedList<String> list2 = new LinkedList<String>();
        File file = new File("data/17.txt");
        FileOutputStream out = new FileOutputStream(new File("data/changes.txt"));   
        if (file.exists() && file.isFile()) {
            try{
                BufferedReader input = new BufferedReader(new FileReader(file));
                String text;// = input.readLine();
                int line = 0;
                while((text = input.readLine()) != null) {
                    text = "ctx_" + line + "," + text;
                    list.add(text);
                    String[] str = text.split(",");
                    String text_ = String.format("%s,%s,%s,%s,%s_%s_%s,%s,%s,%s",
                    		str[0],str[1],str[2],str[3],str[4],str[5],str[6],str[7],str[8],str[9]);
                    long time = convert(str[1]);
                    while(true) {
                        String[] s = list.get(0).split(",");
                        String s_ = String.format("%s,%s,%s,%s,%s_%s_%s,%s,%s,%s",
                        		s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9]);
                        long t = convert(s[1]);
                        if(time - t >= 2000) {
                            String change5 = "-,pat_000," + s_;
                            out.write((change5 + "\n").getBytes());
                            list.remove(0);
                        }
                        else 
                            break;
                    }
                    while(list1.size() != 0) {         
                        String[] s = list1.get(0).split(",");
                        String s_ = String.format("%s,%s,%s,%s,%s_%s_%s,%s,%s,%s",
                        		s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9]);
                        long t = convert(s[1]);
                        if(time - t >= 20000) {
                            String change6 = "-,pat_002," + s_;
                            String change7 = "-,pat_001," + s_;
                            out.write((change6 + "\n").getBytes());
                            out.write((change7 + "\n").getBytes());
                            list1.remove(0);
                        }
                        else
                            break;
                    }
                    while(list2.size() != 0) {
                        String[] s = list2.get(0).split(",");
                        String s_ = String.format("%s,%s,%s,%s,%s_%s_%s,%s,%s,%s",
                        		s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9]);
                        long t = convert(s[1]);
                        int taxiID = Integer.parseInt(s[3]);
                        int digit = taxiID % 10;
                        if(time - t >= 48000) {
                            String change8 = new String();
                            String change9 = new String();
                            if(digit > 4) {
                                change8 = "-,pat_1" + (digit*2 + 1) + "," + s_;
                                change9 = "-,pat_1" + digit*2 + "," + s_;
                            }
                            else {
                                change8 = "-,pat_10" + (digit*2 + 1) + "," + s_;
                                change9 = "-,pat_10" + digit*2 + "," + s_;
                            }
                            out.write((change8 + "\n").getBytes());
                            out.write((change9 + "\n").getBytes());
                            list2.remove(0);
                        }
                        else
                            break;
                    }
                    String change = "+,pat_000," + text_;
                    out.write((change + "\n").getBytes());
                    int taxi = Integer.parseInt(str[3]);
                    int singleDigit = taxi % 10;
                    if(!str[8].equals("0")) {
                        list1.add(text);
                        list2.add(text);
                        String change1 = "+,pat_002," + text_;
                        String change2 = "+,pat_001," + text_;
                        out.write((change1 + "\n").getBytes());
                        out.write((change2 + "\n").getBytes());
                        String change3 = new String();
                        String change4 = new String();
                        if(singleDigit > 4) {
                            change3 = "+,pat_1" + (singleDigit*2 + 1) + "," + text_;
                            change4 = "+,pat_1" + singleDigit*2 + "," + text_;
                        }
                        else {
                            change3 = "+,pat_10" + (singleDigit*2 + 1) + "," + text_;
                            change4 = "+,pat_10" + singleDigit*2 + "," + text_;
                        }
                        out.write((change3 + "\n").getBytes());
                        out.write((change4 + "\n").getBytes());
                    }
                    
                    line++;
                }
                input.close();
            }  
            catch(IOException ioException){
                System.err.println("File Error!");
            }
        }
    }
    public static void main(String[] args) throws Exception{
        trans();
    }
}
