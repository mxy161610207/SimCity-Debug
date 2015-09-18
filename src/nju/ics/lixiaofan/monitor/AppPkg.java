package nju.ics.lixiaofan.monitor;

import java.io.Serializable;

public class AppPkg implements Serializable{
	private static final long serialVersionUID = 1L;
	byte type;
	byte appid;
	String car;
	byte dir;
	byte cmd;
	String loc;
	byte delivid;
	String src, dst;
	byte phase;
	boolean loading;//true:start loading	false:finish loading
	boolean unloading;//true:start unloading	false:finish unloading
	
	public AppPkg() {
	}
	
	//Application ID
	public AppPkg(byte appid) {
		type = 0;
		this.appid = appid;
	}
	
	//car settings
	public void setDir(String car, byte dir) {
		type = 1;
		this.car = car;
		this.dir = dir;
	}
	
	public AppPkg(String car, byte cmd) {
		type = 2;
		this.car = car;
		this.cmd = cmd;
	}
	
	public AppPkg(String car, byte dir, String loc) {
		type = 3;
		this.car = car;
		this.dir = dir;
		this.loc = loc;
	}
	
	public AppPkg(String car, byte dir, String loc, String dst) {
		type = 4;
		this.car = car;
		this.dir = dir;
		this.loc = loc;
		this.dst = dst;
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
	
	public void setDelivery(byte id, String car, String src, String dst, byte phase) {
		type = 7;
		this.delivid = id;
		this.car = car;
		this.src = src;
		this.dst = dst;
		this.phase = phase;
	}
	
	public void setDelivery(String src, String dst) {
		type = 8;
		this.src = src;
		this.dst = dst;
	}
}
