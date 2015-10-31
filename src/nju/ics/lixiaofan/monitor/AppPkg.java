package nju.ics.lixiaofan.monitor;

import java.io.Serializable;

public class AppPkg implements Serializable{
	private static final long serialVersionUID = 1L;
	byte type;
	byte appid;
	String car;
	int dir;
	byte cmd;
	String loc;
	byte delivid;
	String src, dest;
	byte phase;
	boolean loading; //true/false: start/finish loading
	boolean unloading; //true/false: start/finish unloading
	
	String building;
	String btype; //building type
	int block;
	
	public String citizen;
	public String gender;
	public String job;
	public String act;
	
	public AppPkg() {
	}
	
	//Application ID
	public AppPkg(byte appid) {
		type = 0;
		this.appid = appid;
	}
	
	//car settings
	public void setDir(String car, int dir) {
		type = 1;
		this.car = car;
		this.dir = dir;
	}
	
	public void setCmd(String car, byte cmd) {
		type = 2;
		this.car = car;
		this.cmd = cmd;
	}
	
	public void setCar(String car, int dir, String loc) {
		type = 3;
		this.car = car;
		this.dir = dir;
		this.loc = loc;
	}
	
	public void setCar(String car, byte dir, String loc, String dst) {
		type = 4;
		this.car = car;
		this.dir = dir;
		this.loc = loc;
		this.dest = dst;
	}
	
	public void setLoading(String car, boolean loading){
		type = 5;
		this.car = car;
		this.loading = loading;
	}
	
	public void setUnloading(String car, boolean unloading){
		type = 6;
		this.car = car;
		this.unloading = unloading;
	}
	
	public void setDelivery(byte id, String car, String src, String dest, byte phase) {
		type = 7;
		this.delivid = id;
		this.car = car;
		this.src = src;
		this.dest = dest;
		this.phase = phase;
	}
	
	public void setDelivery(String src, String dest) {
		type = 8;
		this.src = src;
		this.dest = dest;
	}
	
	public void setBuilding(String building, String btype, int block){
		type = 9;
		this.building = building;
		this.btype = btype;
		this.block = block;
	}
	
	public void setCitizen(String citizen, String gender, String job){
		type = 10;
		this.citizen = citizen;
		this.gender = gender;
		this.job = job;
	}
}
