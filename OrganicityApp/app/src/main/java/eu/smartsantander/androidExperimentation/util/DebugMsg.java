package eu.smartsantander.androidExperimentation.util;

import java.util.Date;

public class DebugMsg {
	
	private String msg;
	private Date date;
	
	public DebugMsg(String m, Date d) {
		msg = m;
		date = d;
	}
	
	public DebugMsg(String m) {
		msg = m;
		date = new Date();
	}
	
	public DebugMsg() {
		msg = "Default debug message";
		date = new Date();
	}
	
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
		
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	

}
