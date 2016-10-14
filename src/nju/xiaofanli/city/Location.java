package nju.xiaofanli.city;

public abstract class Location {
	public int id;
	public String name;
	
	public static Location LocOf(String name){
		if(name == null)
			return null;
		for(Road road : TrafficMap.roads.values())
			if(road.name.equals(name))
				return road;
		for(Building building : TrafficMap.buildings.values())
			if(building.name.equals(name))
				return building;
		return null;
	}
}
