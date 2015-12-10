/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.dataLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nju.ics.lixiaofan.consistency.context.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author bingying
 * 获取每类context的框架内容
 */

public class PatternLoader {
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(RuleLoader.class.getName());
    
    private static Object parseElement(Element element) {
        String tagName = element.getNodeName();
		NodeList children = element.getChildNodes();
		
		if(tagName.equals("contexts")) {//context集合
			ArrayList<Pattern> contexts = new ArrayList<Pattern>();
			//子节点处理
            for(int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //获得结点的类型
                    short nodeType = node.getNodeType();
                    if(nodeType == Node.ELEMENT_NODE) {
                        //是元素，继续递归
                        Pattern context = (Pattern)parseElement((Element)node);
                        contexts.add(context);
                    }
            }
            return contexts;
        }
        if(tagName.equals("context")) {//单个context
            String name = new String();
            NamedNodeMap map = element.getAttributes();
            //如果该元素存在属性
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                    //获得该元素的每一个属性
                    Attr attr = (Attr)map.item(i);	
                    String attrName = attr.getName();
                    String attrValue = attr.getValue();
                    if(attrName.equals("name")) {
                        name = attrValue;
                    }
                }
            }
            Pattern context = new Pattern(name);
            //子节点处理
            for(int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                //获得结点的类型
                short nodeType = node.getNodeType();
                if(nodeType == Node.ELEMENT_NODE) {
                    //是元素，继续递归
                    String field = (String)parseElement((Element)node);
                    context.addField(field);
                }
            }
            return context;
        }
        if(tagName.equals("field")) {
            String field = new String();
            //属性处理
            NamedNodeMap map = element.getAttributes();
            //如果该元素存在属性
            if(map != null) {
                //获得该元素的第一个属性，name
                Attr attr = (Attr)map.item(0);	
                String attrName = attr.getName();
                if(attrName.equals("name")) {
                    field = attr.getValue();
                }
            }
            return field;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
	public static ArrayList<Pattern> parserXml(String fileName) { 
    	ArrayList<Pattern> contexts = new ArrayList<Pattern>();
        try { 
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document document = db.parse(fileName); 
            
            //获得根元素结点
            Element root = document.getDocumentElement();
            contexts = (ArrayList<Pattern>)parseElement(root);
            //System.out.println("contexts解析完毕"); 
            
        } catch (FileNotFoundException e) { 
            System.out.println(e.getMessage()); 
        } catch (ParserConfigurationException e) { 
            System.out.println(e.getMessage()); 
        } catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return contexts;
    } 
    
}
