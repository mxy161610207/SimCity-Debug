package nju.xiaofanli.application.monitor;

import nju.xiaofanli.dashboard.TrafficMap;

import java.io.Serializable;

public class AppPkg implements Serializable{
	private static final long serialVersionUID = 1L;
	int type;
	int appid;
	String car;
	int dir;
	int cmd;
	String loc, realLoc;
	int delivid;
	String src, dest;
	int phase;
	boolean loading; //true/false: start/finish loading
	boolean unloading; //true/false: start/finish unloading

	String building;
	String btype; //building type
	int block;

	public String citizen;
	public String gender;
	public String job;
	public int color;
	public String act;
	public double ratioX, ratioY;
	public boolean isVisible;

	int ctxType;
	String sensor, road;
	boolean isResolutionEnabled;

	public AppPkg() {
	}

	//Application ID
	public AppPkg(int appid) {
		type = 0;
		this.appid = appid;
	}

	//car settings
	public AppPkg setDir(String car, TrafficMap.Direction dir) {
		type = 1;
		this.car = car;
		this.dir = dir.ordinal();
		return this;
	}

	public AppPkg setCmd(String car, int cmd) {
		type = 2;
		this.car = car;
		this.cmd = cmd;
		return this;
	}

	public AppPkg setCar(String car, TrafficMap.Direction dir, String loc) {
		type = 3;
		this.car = car;
		this.dir = dir.ordinal();
		this.loc = loc;
		return this;
	}

	public AppPkg setCar(String car, TrafficMap.Direction dir, String loc, String dst) {
		type = 4;
		this.car = car;
		this.dir = dir.ordinal();
		this.loc = loc;
		this.dest = dst;
		return this;
	}

	public AppPkg setLoading(String car, boolean loading){
		type = 5;
		this.car = car;
		this.loading = loading;
		return this;
	}

	public AppPkg setUnloading(String car, boolean unloading){
		type = 6;
		this.car = car;
		this.unloading = unloading;
		return this;
	}

	public AppPkg setDelivery(int id, String car, String src, String dest, int phase) {
		type = 7;
		this.delivid = id;
		this.car = car;
		this.src = src;
		this.dest = dest;
		this.phase = phase;
		return this;
	}

	public AppPkg setDelivery(String src, String dest) {
		type = 8;
		this.src = src;
		this.dest = dest;
		return this;
	}

	public AppPkg setBuilding(String building, String btype, int block){
		type = 9;
		this.building = building;
		this.btype = btype;
		this.block = block;
		return this;
	}

	public AppPkg setCitizen(String citizen, String gender, String job, int color){
		type = 10;
		this.citizen = citizen;
		this.gender = gender;
		this.job = job;
		this.color = color;
		return this;
	}

	public AppPkg setCitizen(String citizen, String act){
		type = 11;
		this.citizen = citizen;
		this.act = act;
		return this;
	}

	public AppPkg setCitizen(String citizen, double x, double y){
		type = 12;
		this.citizen = citizen;
		this.ratioX = x;
		this.ratioY = y;
		return this;
	}

	public AppPkg setCitizen(String citizen, boolean visible){
		type = 13;
		this.citizen = citizen;
		this.isVisible = visible;
		return this;
	}

	public AppPkg setCarRealLoc(String car, String realLoc){
		type = 14;
		this.car = car;
		this.realLoc = realLoc;
		return this;
	}

	public AppPkg setBalloon(int ctxType, String sensor, String car, boolean isResolutionEnabled){
		type = 15;
		this.ctxType = ctxType;
		this.sensor = sensor;
		this.car = car;
		this.isResolutionEnabled = isResolutionEnabled;
		return this;
	}
}
