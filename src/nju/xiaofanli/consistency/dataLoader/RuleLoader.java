package nju.xiaofanli.consistency.dataLoader;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.formula.*;
import nju.xiaofanli.consistency.middleware.Middleware;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** 
* 
* @author bingying
* 
* ��ȡԼ���ļ�����ȡrules
*/ 

public class RuleLoader {
    //Rule rules;
	private static Set<Rule> rules = new HashSet<>();

    private static Object parseElement(Element element, Rule argRule) {
        String tagName = element.getNodeName();
        NodeList children = element.getChildNodes();

        switch (tagName) {
            case "rules": //Լ������
                //�ӽڵ㴦��
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //��ý�������
                    short nodeType = node.getNodeType();
                    if (nodeType == Node.ELEMENT_NODE) {
                        //��Ԫ�أ������ݹ�
                        Rule rule = (Rule) parseElement((Element) node, null);
                        rules.add(rule);
                    }
                }
                return rules;
            case "rule": {//����Լ��
                //��ȡid�ţ�Լ����ţ�
                Node idNode = children.item(1);
                NodeList subChildren = idNode.getChildNodes();
                String name = subChildren.item(0).getNodeValue();
                Rule rule = new Rule(name);
                //explanation
                Node explanationNode = children.item(3);
                if (explanationNode.hasAttributes()) {
                    NamedNodeMap map = explanationNode.getAttributes();
                    Attr attr = (Attr) map.getNamedItem("en");
                    if (attr != null)
                        rule.setExplanation(attr.getValue(), true);
                    attr = (Attr) map.getNamedItem("ch");
                    if (attr != null)
                        rule.setExplanation(attr.getValue(), false);
                }
                //formula
                Node formulaNode = children.item(5);
                Formula formula = (Formula) parseElement((Element) formulaNode, rule);
                rule.setFormula(formula);
                return rule;
            }
            case "formula": {//��Լ����һ���߼���ʽ
                Node kindNode = children.item(1);
                return parseElement((Element) kindNode, argRule);
            }
            case "forall": {//ȫ�����ʹ�ʽ
                ForallFormula formula = new ForallFormula(tagName);

                NamedNodeMap map = element.getAttributes();
                String var = null;
                String pat = null;
                //�����Ԫ�ش�������
                if (map != null) {
                    for (int i = 0; i < map.getLength(); i++) {
                        //��ø�Ԫ�ص�ÿһ������
                        Attr attr = (Attr) map.item(i);
                        String attrName = attr.getName();
                        String attrValue = attr.getValue();
                        if (attrName.equals("var")) {
                            var = attrValue;
                        }
                        if (attrName.equals("in")) {
                            pat = attrValue;
                        }
                    }
                }
                formula.setPattern(var, Middleware.getPatterns().get(pat));
                Middleware.getPatterns().get(pat).setRule(argRule);

                NodeList subChildren = element.getChildNodes();
                Node sub = subChildren.item(1);
                Formula subFormula = (Formula) parseElement((Element) sub, argRule);
                formula.setSubFormula(subFormula);
                return formula;
            }
            case "exists": {//�������ʹ�ʽ
                ExistsFormula formula = new ExistsFormula(tagName);

                NamedNodeMap map = element.getAttributes();
                String var = null;
                String pat = null;
                //�����Ԫ�ش�������
                if (map != null) {
                    for (int i = 0; i < map.getLength(); i++) {
                        //��ø�Ԫ�ص�ÿһ������
                        Attr attr = (Attr) map.item(i);
                        String attrName = attr.getName();
                        String attrValue = attr.getValue();
                        if (attrName.equals("var")) {
                            var = attrValue;
                        }
                        if (attrName.equals("in")) {
                            pat = attrValue;
                        }
                    }
                }
                formula.setPattern(var, Middleware.getPatterns().get(pat));
                Middleware.getPatterns().get(pat).setRule(argRule);

                NodeList subChildren = element.getChildNodes();
                Node sub = subChildren.item(1);
                Formula subFormula = (Formula) parseElement((Element) sub, argRule);
                formula.setSubFormula(subFormula);
                return formula;
            }
            case "and": {
                AndFormula formula = new AndFormula(tagName);
                NodeList subChildren = element.getChildNodes();
                Node first = subChildren.item(1);
                Formula firstFormula = (Formula) parseElement((Element) first, argRule);
                Node second = subChildren.item(3);
                Formula secondFormula = (Formula) parseElement((Element) second, argRule);
                formula.setSubFormula(firstFormula, secondFormula);
                return formula;
            }
            case "or": {
                OrFormula formula = new OrFormula(tagName);
                NodeList subChildren = element.getChildNodes();
                Node first = subChildren.item(1);
                Formula firstFormula = (Formula) parseElement((Element) first, argRule);
                Node second = subChildren.item(3);
                Formula secondFormula = (Formula) parseElement((Element) second, argRule);
                formula.setSubFormula(firstFormula, secondFormula);
                return formula;
            }
            case "implies": {
                ImpliesFormula formula = new ImpliesFormula(tagName);
                NodeList subChildren = element.getChildNodes();
                Node first = subChildren.item(1);
                Formula firstFormula = (Formula) parseElement((Element) first, argRule);
                Node second = subChildren.item(3);
                Formula secondFormula = (Formula) parseElement((Element) second, argRule);
                formula.setSubFormula(firstFormula, secondFormula);
                return formula;
            }
            case "not": {
                NotFormula formula = new NotFormula(tagName);
                NodeList subChildren = element.getChildNodes();
                Node sub = subChildren.item(1);
                Formula subFormula = (Formula) parseElement((Element) sub, argRule);
                formula.setSubFormula(subFormula);
                return formula;
            }
            case "bfunc": {
                String id = "";
                NamedNodeMap map = element.getAttributes();
                //�����Ԫ�ش�������
                if (map != null) {
                    for (int i = 0; i < map.getLength(); i++) {
                        //��ø�Ԫ�ص�ÿһ������
                        Attr attr = (Attr) map.item(i);
                        String attrName = attr.getName();
                        String attrValue = attr.getValue();
                        if (attrName.equals("id")) {
                            id = attrValue;
                        }
                    }
                }
                BFunc formula = new BFunc(id);
                //�ӽڵ㴦��
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    //��ý�������
                    short nodeType = node.getNodeType();
                    if (nodeType == Node.ELEMENT_NODE) {
                        //��Ԫ�أ������ݹ�
                        String[] param = (String[]) parseElement((Element) node, argRule);
                        if(param == null)
                            throw new NullPointerException();
                        formula.addParam(Integer.parseInt(param[0]), param[1], param[2]);
                    }
                }
                return formula;
            }
            case "param": {
                String[] param = new String[3];
                //���Դ���
                NamedNodeMap map = element.getAttributes();
                //�����Ԫ�ش�������
                if (map != null) {
                    for (int i = 0; i < map.getLength(); i++) {
                        //��ø�Ԫ�ص�ÿһ������
                        Attr attr = (Attr) map.item(i);
                        String attrName = attr.getName();
                        String attrValue = attr.getValue();
                        if (attrName.equals("pos")) {
                            param[0] = attrValue;
                        }
                        if (attrName.equals("var")) {
                            param[1] = attrValue;
                        }
                        if (attrName.equals("field")) {
                            param[2] = attrValue;
                        }
                    }
                }
                return param;
            }
        }
        return null;
    }
    public static Set<Rule> parserXml(String fileName) {
        try { 
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document document = db.parse(fileName);
            
            //��ø�Ԫ�ؽ��
            Element root = document.getDocumentElement();
            parseElement(root, null);
            //System.out.println("constraints�������"); 
            
        } catch (FileNotFoundException | ParserConfigurationException e) {
            System.out.println(e.getMessage()); 
        } catch (SAXException | IOException e) {
			e.printStackTrace();
		}
        return rules;
    } 
} 