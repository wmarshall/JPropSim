package com.wcmarshall.jpropsim;

public class Cog {

	private int id;

	private boolean zflag, cflag;

	public Cog(int id) {
		this.id = id;
	}

	public boolean getZFlag() {
		return zflag;
	}

	public void setZFlag(boolean zflag) {
		this.zflag = zflag;
	}

	public boolean getCFlag() {
		return cflag;
	}

	public void setCFlag(boolean cflag) {
		this.cflag = cflag;
	}

}
