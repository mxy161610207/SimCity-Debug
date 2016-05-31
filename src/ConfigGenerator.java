import java.io.FileWriter;
import java.io.IOException;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Citizen;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ConfigGenerator {
	public static final String configFile = "config.xml";
	public static void main(String[] args) throws IOException{
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("config");
		//car
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
		//building
		Element building = root.addElement("building");
		building.addAttribute("name", "Stark Industries");
		building.addAttribute("type", Building.Type.StarkIndustries.toString());
		building.addAttribute("loc", "9");
		
		building = root.addElement("building");
		building.addAttribute("name", "Hospital");
		building.addAttribute("type", Building.Type.Hospital.toString());
		building.addAttribute("loc", "6");
		
		building = root.addElement("building");
		building.addAttribute("name", "School");
		building.addAttribute("type", Building.Type.School.toString());
		building.addAttribute("loc", "5");
		
		building = root.addElement("building");
		building.addAttribute("name", "Police Station");
		building.addAttribute("type", Building.Type.PoliceStation.toString());
		building.addAttribute("loc", "10");
		
		building = root.addElement("building");
		building.addAttribute("name", "Restaurant");
		building.addAttribute("type", Building.Type.Restaurant.toString());
		building.addAttribute("loc", "12");
		
		//citizen
		Element citizen = root.addElement("citizen");
		citizen.addAttribute("name", "Tony Stark");
		citizen.addAttribute("gender", Citizen.Gender.Male.toString());
		citizen.addAttribute("job", Citizen.Job.IronMan.toString());
		
		//Brick
		for(int i = 0;i < 10;i++){
			Element brick = root.addElement("brick");
			brick.addAttribute("name", "" + i);
			brick.addAttribute("address", "192.168.1.11" + i);
		}
		
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(new FileWriter(configFile), format);
		writer.write(doc);
		writer.close();
	}
}
