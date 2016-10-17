package nju.xiaofanli.util;
import java.io.FileWriter;
import java.io.IOException;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.dashboard.Building;
import nju.xiaofanli.dashboard.Citizen;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ConfigGenerator {
	public static final String FILE = "config.xml";
	public static void main(String[] args) throws IOException{
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("config");
		//car
		Element car = root.addElement("car");
		car.addAttribute("name", Car.BLACK);
		car.addAttribute("loc", "Street 18");
		car.addAttribute("url", "btspp://00066649A8C4:1;authenticate=true;encrypt=true");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.GREEN);
		car.addAttribute("loc", "Crossroad 6");
        car.addAttribute("url", "btspp://000666459D35:1;authenticate=true;encrypt=true");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.ORANGE);
		car.addAttribute("loc", "Crossroad 7");
        car.addAttribute("url", "btspp://00066649960C:1;authenticate=true;encrypt=true");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.RED);
		car.addAttribute("loc", "Street 24");
        car.addAttribute("url", "btspp://000666619F38:1;authenticate=true;encrypt=true");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.SILVER);
		car.addAttribute("loc", "Street 23");
        car.addAttribute("url", "btspp://00066661A901:1;authenticate=true;encrypt=true");
		
		car = root.addElement("car");
		car.addAttribute("name", Car.WHITE);
		car.addAttribute("loc", "Street 28");
        car.addAttribute("url", "btspp://00066661AA61:1;authenticate=true;encrypt=true");

		//Brick
		for(int i = 0;i < 10;i++){
			Element brick = root.addElement("brick");
			brick.addAttribute("name", "" + i);
			brick.addAttribute("address", "192.168.1.11" + i);
		}

        //building
        Element building = root.addElement("building");
        building.addAttribute("name", "Stark Industries");
        building.addAttribute("type", Building.Type.StarkIndustries.toString());
        building.addAttribute("loc", "9");
        building.addAttribute("icon", "res/stark_industries.png");

        building = root.addElement("building");
        building.addAttribute("name", "Hospital");
        building.addAttribute("type", Building.Type.Hospital.toString());
        building.addAttribute("loc", "6");
        building.addAttribute("icon", "res/hospital.png");

        building = root.addElement("building");
        building.addAttribute("name", "School");
        building.addAttribute("type", Building.Type.School.toString());
        building.addAttribute("loc", "5");
        building.addAttribute("icon", "res/nju.png");

        building = root.addElement("building");
        building.addAttribute("name", "Police Station");
        building.addAttribute("type", Building.Type.PoliceStation.toString());
        building.addAttribute("loc", "10");
        building.addAttribute("icon", "res/shield.png");

        building = root.addElement("building");
        building.addAttribute("name", "Restaurant");
        building.addAttribute("type", Building.Type.Restaurant.toString());
        building.addAttribute("loc", "13");
        building.addAttribute("icon", "res/java.png");

        //citizen
        Element citizen = root.addElement("citizen");
        citizen.addAttribute("name", "Tony Stark");
        citizen.addAttribute("gender", Citizen.Gender.Male.toString());
        citizen.addAttribute("job", Citizen.Job.SuperHero.toString());
        building.addAttribute("icon", "res/ironman.png");

        citizen = root.addElement("citizen");
        citizen.addAttribute("name", "Tony Stark");
        citizen.addAttribute("gender", Citizen.Gender.Male.toString());
        citizen.addAttribute("job", Citizen.Job.SuperHero.toString());
        building.addAttribute("icon", "res/ironman.png");

        citizen = root.addElement("citizen");
        citizen.addAttribute("name", "Wade Wilson");
        citizen.addAttribute("gender", Citizen.Gender.Male.toString());
        citizen.addAttribute("job", Citizen.Job.SuperHero.toString());
        building.addAttribute("icon", "res/deadpool.png");
		
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(new FileWriter(FILE), format);
		writer.write(doc);
		writer.close();
	}
}
