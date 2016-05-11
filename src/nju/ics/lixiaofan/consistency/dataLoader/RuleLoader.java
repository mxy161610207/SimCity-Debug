package nju.ics.lixiaofan.consistency.dataLoader;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nju.ics.lixiaofan.consistency.context.Rule;
import nju.ics.lixiaofan.consistency.formula.*;
import nju.ics.lixiaofan.consistency.middleware.Middleware;

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
* 
* ��ȡԼ���ļ�����ȡrules
*/ 

public class RuleLoader {
    //Rule rules;
	private static HashSet<Rule> rules = new HashSet<Rule>();
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(RuleLoader.class.getName());
    
    private static Object parseElement(Element element, String ruleName) {
        String tagName = element.getNodeName();
        NodeList children = element.getChildNodes();
	
        if(tagName.equals("rules")) {//Լ������
            //�ӽڵ㴦��
            for(int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //��ý�������
                    short nodeType = node.getNodeType();
                    if(nodeType == Node.ELEMENT_NODE) {
                        //��Ԫ�أ������ݹ�
                        Rule rule = (Rule)parseElement((Element)node, null);
                        rules.add(rule);
                    }
            }
            return rules;
        }
        else if(tagName.equals("rule")) {//����Լ��
            //��ȡid�ţ�Լ����ţ�
            Node idNode = children.item(1);
            NodeList subChildren = idNode.getChildNodes();
            String name = subChildren.item(0).getNodeValue();
            Rule rule = new Rule(name);
            //formula
            Node formulaNode = children.item(3);
            Formula formula = (Formula)parseElement((Element)formulaNode, name);
            rule.setFormula(formula);
            return rule;
        }
        else if(tagName.equals("formula")) {//��Լ����һ���߼���ʽ
            Node kindNode = children.item(1);
            Formula formula = (Formula)parseElement((Element)kindNode, ruleName);
            return formula;
        }
        else if(tagName.equals("forall")) {//ȫ�����ʹ�ʽ
            ForallFormula formula = new ForallFormula(tagName);
                
            NamedNodeMap map = element.getAttributes();
            String var = null;
            String pat = null;
            //�����Ԫ�ش�������
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                    //��ø�Ԫ�ص�ÿһ������
                    Attr attr = (Attr)map.item(i);	
                    String attrName = attr.getName();
                    String attrValue = attr.getValue();
                    if(attrName.equals("var")) {
                        var = attrValue;
                    }
                    if(attrName.equals("in")) {
                        pat = attrValue;
                    }
                }
            }
            formula.setPattern(var, Middleware.getPatterns().get(pat));
            Middleware.getPatterns().get(pat).setRule(ruleName);
            
            NodeList subChildren = element.getChildNodes();
            Node sub = subChildren.item(1);
            Formula subFormula = (Formula)parseElement((Element)sub, ruleName);
            formula.setSubFormula(subFormula);
            return formula;
        }
        else if(tagName.equals("exists")) {//�������ʹ�ʽ
            ExistsFormula formula = new ExistsFormula(tagName);
                
            NamedNodeMap map = element.getAttributes();
            String var = null;
            String pat = null;
            //�����Ԫ�ش�������
            if(map != null) {
                    for(int i = 0; i < map.getLength(); i++) {
                            //��ø�Ԫ�ص�ÿһ������
                            Attr attr = (Attr)map.item(i);	
                            String attrName = attr.getName();
                            String attrValue = attr.getValue();
                            if(attrName.equals("var")) {
                                var = attrValue;
                            }
                            if(attrName.equals("in")) {
                                pat = attrValue;
                            }
                    }
            }
            formula.setPattern(var, Middleware.getPatterns().get(pat));
            Middleware.getPatterns().get(pat).setRule(ruleName);
            
            NodeList subChildren = element.getChildNodes();
            Node sub = subChildren.item(1);
            Formula subFormula = (Formula)parseElement((Element)sub, ruleName);
            formula.setSubFormula(subFormula);
            return formula;
        }
        else if(tagName.equals("and")) {
            AndFormula formula = new AndFormula(tagName);
            NodeList subChildren = element.getChildNodes();
            Node first = subChildren.item(1);
            Formula firstFormula = (Formula)parseElement((Element)first, ruleName);
            Node second = subChildren.item(3);
            Formula secondFormula = (Formula)parseElement((Element)second, ruleName);
            formula.setSubFormula(firstFormula,secondFormula);
            return formula;
        }
        else if(tagName.equals("or")) {
            OrFormula formula = new OrFormula(tagName);
            NodeList subChildren = element.getChildNodes();
            Node first = subChildren.item(1);
            Formula firstFormula = (Formula)parseElement((Element)first, ruleName);
            Node second = subChildren.item(3);
            Formula secondFormula = (Formula)parseElement((Element)second, ruleName);
            formula.setSubFormula(firstFormula,secondFormula);
            return formula;
        }
        else if(tagName.equals("implies")) {
            ImpliesFormula formula = new ImpliesFormula(tagName);
            NodeList subChildren = element.getChildNodes();
            Node first = subChildren.item(1);
            Formula firstFormula = (Formula)parseElement((Element)first, ruleName);
            Node second = subChildren.item(3);
            Formula secondFormula = (Formula)parseElement((Element)second, ruleName);
            formula.setSubFormula(firstFormula,secondFormula);
            return formula;
        }
        else if(tagName.equals("not")) {
            NotFormula formula = new NotFormula(tagName);
            NodeList subChildren = element.getChildNodes();
            Node sub = subChildren.item(1);
            Formula subFormula = (Formula)parseElement((Element)sub, ruleName);
            formula.setSubFormula(subFormula);
            return formula;
        }
        else if(tagName.equals("bfunc")) {
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
            BFunc formula = new BFunc(name);
            //�ӽڵ㴦��
            for(int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                //��ý�������
                short nodeType = node.getNodeType();
                if(nodeType == Node.ELEMENT_NODE) {
                    //��Ԫ�أ������ݹ�
                    String[] param = (String[])parseElement((Element)node, ruleName);
                    formula.addParam(Integer.parseInt(param[0]), param[1], param[2]);
                }
            }
            return formula;
        }
        else if(tagName.equals("param")) {
            String[] param = new String[3];
            //���Դ���
            NamedNodeMap map = element.getAttributes();
            //�����Ԫ�ش�������
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                        //��ø�Ԫ�ص�ÿһ������
                        Attr attr = (Attr)map.item(i);	
                        String attrName = attr.getName();
                        String attrValue = attr.getValue();
                        if(attrName.equals("pos")) {
                            param[0] = attrValue;
                        }
                        if(attrName.equals("var")) {
                            param[1] = attrValue;
                        }	
                        if(attrName.equals("field")) {
                            param[2] = attrValue;
                        }
                }
            }
            return param;
        }
        return null;
    }
    public static HashSet<Rule> parserXml(String fileName) { 
        try { 
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document document = db.parse(fileName); 
            
            //��ø�Ԫ�ؽ��
            Element root = document.getDocumentElement();
            parseElement(root, null);
            //System.out.println("constraints�������"); 
            
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
        return rules;
    } 
} 