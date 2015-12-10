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
 * ��ȡÿ��context�Ŀ������
 */

public class PatternLoader {
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(RuleLoader.class.getName());
    
    private static Object parseElement(Element element) {
        String tagName = element.getNodeName();
		NodeList children = element.getChildNodes();
		
		if(tagName.equals("contexts")) {//context����
			ArrayList<Pattern> contexts = new ArrayList<Pattern>();
			//�ӽڵ㴦��
            for(int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //��ý�������
                    short nodeType = node.getNodeType();
                    if(nodeType == Node.ELEMENT_NODE) {
                        //��Ԫ�أ������ݹ�
                        Pattern context = (Pattern)parseElement((Element)node);
                        contexts.add(context);
                    }
            }
            return contexts;
        }
        if(tagName.equals("context")) {//����context
            String name = new String();
            NamedNodeMap map = element.getAttributes();
            //�����Ԫ�ش�������
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                    //��ø�Ԫ�ص�ÿһ������
                    Attr attr = (Attr)map.item(i);	
                    String attrName = attr.getName();
                    String attrValue = attr.getValue();
                    if(attrName.equals("name")) {
                        name = attrValue;
                    }
                }
            }
            Pattern context = new Pattern(name);
            //�ӽڵ㴦��
            for(int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                //��ý�������
                short nodeType = node.getNodeType();
                if(nodeType == Node.ELEMENT_NODE) {
                    //��Ԫ�أ������ݹ�
                    String field = (String)parseElement((Element)node);
                    context.addField(field);
                }
            }
            return context;
        }
        if(tagName.equals("field")) {
            String field = new String();
            //���Դ���
            NamedNodeMap map = element.getAttributes();
            //�����Ԫ�ش�������
            if(map != null) {
                //��ø�Ԫ�صĵ�һ�����ԣ�name
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
            
            //��ø�Ԫ�ؽ��
            Element root = document.getDocumentElement();
            contexts = (ArrayList<Pattern>)parseElement(root);
            //System.out.println("contexts�������"); 
            
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
