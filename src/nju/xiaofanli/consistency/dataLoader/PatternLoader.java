/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.dataLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nju.xiaofanli.consistency.context.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    
/*    private static Object parseElement(Element element) {
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
            Pattern pattern = new Pattern(name);
            //�ӽڵ㴦��
            for(int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                //��ý�������
                short nodeType = node.getNodeType();
                if(nodeType == Node.ELEMENT_NODE) {
                    //��Ԫ�أ������ݹ�
                    String field = (String)parseElement((Element)node);
                    pattern.addField(field);
                }
            }
            return pattern;
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
    }*/

    public static Set<Pattern> parserXml(String fileName) {
    	Set<Pattern> patterns = new HashSet<>();
        try { 
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document document = db.parse(fileName); 
            
            //��ø�Ԫ�ؽ��
//            Element root = document.getDocumentElement();
//            patterns = (ArrayList<Pattern>)parseElement(root);
            NodeList children = document.getElementsByTagName("pattern");
        	for(int i = 0;i < children.getLength();i++){
        		Element child = (Element) children.item(i);
        		Pattern pattern = new Pattern(child.getElementsByTagName("id").item(0).getFirstChild().getNodeValue());
    			NodeList fields = child.getChildNodes();
    			for(int j = 1;j < fields.getLength();j += 2){
    				Node field = fields.item(j);
    				if(!field.getNodeName().equals("id"))
    					pattern.addField(field.getNodeName(), field.getFirstChild().getNodeValue());
    			}
    			patterns.add(pattern);
        	}
            
        } catch (FileNotFoundException | ParserConfigurationException e) {
            System.out.println(e.getMessage()); 
        } catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		return patterns;
    } 
    
}
