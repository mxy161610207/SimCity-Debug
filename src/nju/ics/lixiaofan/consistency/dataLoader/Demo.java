/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.dataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
/**
 *
 * @author bingying
 * 获取单条contextchange
 */
public class Demo extends Changes{
    private static int pointer;
    private static LinkedList<String> list = new LinkedList<String>();
     
    public Demo(String filename) {
        pointer = 0;
        File file = new File(filename);
        
        if (file.exists() && file.isFile()) {
            try{
                BufferedReader input = new BufferedReader(new FileReader(file));
                String text;
                while((text = input.readLine()) != null)
                    list.add(text);
                input.close();
            }  
            catch(IOException ioException){
                System.err.println("File Error!");
            }
        }
    }

    @Override
    public boolean hasNextChange() {
        if(pointer < list.size())
            return true;
        else
            return false;
    }

    @Override
    public ContextChange nextChange() {
        String changeString = list.get(pointer);
        pointer++;
        ContextChange change = new ContextChange();
        String[] s = changeString.split(",");
        if(s[0].matches("[+]"))
            change.setType(ContextChange.ADDITION);
        else if(s[0].matches("[-]"))
            change.setType(ContextChange.DELETION);
        else if(s[0].matches("[#]"))
            change.setType(ContextChange.UPDATE);
//        change.setPatternName(s[1]);
//        change.setContextName(s[2]);
        
        Pattern pat = Middleware.getPatterns().get(s[1]);
        Context ctx = new Context();
        for(int i = 0;i < pat.getFields().size();i ++) {
            ctx.addField(pat.getFields().get(i), s[i + 2]);
        }
        ctx.setPattern(pat);
        ctx.setName(s[2]);
        
        change.setPattern(pat);
        change.setContext(ctx);
        return change;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getChanges() {
        return list.size();
    }
}
