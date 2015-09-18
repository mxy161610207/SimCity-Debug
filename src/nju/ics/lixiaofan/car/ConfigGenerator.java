package nju.ics.lixiaofan.car;

import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ConfigGenerator {
	public static String filename = "Config.xml";
	public static void main(String[] args) throws IOException {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("config");
		
		Element car = root.addElement("car");
		car.addAttribute("name", Car.BLACK);
		car.addAttribute("loc", "Street 18");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.GREEN);
		car.addAttribute("loc", "Crossing 6");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.ORANGE);
		car.addAttribute("loc", "Crossing 7");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.RED);
		car.addAttribute("loc", "Street 24");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.SILVER);
		car.addAttribute("loc", "Street 23");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.WHITE);
		car.addAttribute("loc", "Street 28");
		
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(new FileWriter(filename), format);
		writer.write(doc);
		writer.close();
	}
}
