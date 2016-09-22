package nju.xiaofanli.city;

public abstract class Location {
	public int id;
	public String name;
	
	public static Location LocOf(String name){
		if(name == null)
			return null;
		for(Section s : TrafficMap.sections.values())
			if(s.name.equals(name))
				return s;
		for(Building b : TrafficMap.buildings.values())
			if(b.name.equals(name))
				return b;
		return null;
	}
}
