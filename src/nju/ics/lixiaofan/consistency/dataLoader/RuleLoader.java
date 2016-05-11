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
* 读取约束文件，获取rules
*/ 

public class RuleLoader {
    //Rule rules;
	private static HashSet<Rule> rules = new HashSet<Rule>();
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(RuleLoader.class.getName());
    
    private static Object parseElement(Element element, String ruleName) {
        String tagName = element.getNodeName();
        NodeList children = element.getChildNodes();
	
        if(tagName.equals("rules")) {//约束集合
            //子节点处理
            for(int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //获得结点的类型
                    short nodeType = node.getNodeType();
                    if(nodeType == Node.ELEMENT_NODE) {
                        //是元素，继续递归
                        Rule rule = (Rule)parseElement((Element)node, null);
                        rules.add(rule);
                    }
            }
            return rules;
        }
        else if(tagName.equals("rule")) {//单个约束
            //获取id号（约束编号）
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
        else if(tagName.equals("formula")) {//该约束的一阶逻辑公式
            Node kindNode = children.item(1);
            Formula formula = (Formula)parseElement((Element)kindNode, ruleName);
            return formula;
        }
        else if(tagName.equals("forall")) {//全称量词公式
            ForallFormula formula = new ForallFormula(tagName);
                
            NamedNodeMap map = element.getAttributes();
            String var = null;
            String pat = null;
            //如果该元素存在属性
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                    //获得该元素的每一个属性
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
        else if(tagName.equals("exists")) {//存在量词公式
            ExistsFormula formula = new ExistsFormula(tagName);
                
            NamedNodeMap map = element.getAttributes();
            String var = null;
            String pat = null;
            //如果该元素存在属性
            if(map != null) {
                    for(int i = 0; i < map.getLength(); i++) {
                            //获得该元素的每一个属性
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
            BFunc formula = new BFunc(name);
            //子节点处理
            for(int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                //获得结点的类型
                short nodeType = node.getNodeType();
                if(nodeType == Node.ELEMENT_NODE) {
                    //是元素，继续递归
                    String[] param = (String[])parseElement((Element)node, ruleName);
                    formula.addParam(Integer.parseInt(param[0]), param[1], param[2]);
                }
            }
            return formula;
        }
        else if(tagName.equals("param")) {
            String[] param = new String[3];
            //属性处理
            NamedNodeMap map = element.getAttributes();
            //如果该元素存在属性
            if(map != null) {
                for(int i = 0; i < map.getLength(); i++) {
                        //获得该元素的每一个属性
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
            
            //获得根元素结点
            Element root = document.getDocumentElement();
            parseElement(root, null);
            //System.out.println("constraints解析完毕"); 
            
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