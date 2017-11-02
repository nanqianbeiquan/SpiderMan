package gs;

import tools.DataModel;

public class GsModel{

	public String abstractInfo=null;
	
	public String orgName=null;
	public String code=null;
	public int updateStatus=-1;
	public String provName=null;
	
	public GsModel(String orgName,String provName) {
		this.orgName=orgName;
		this.provName=provName;
	}
	
	public void setUpdateStatus(int status)
	{
		this.updateStatus=status;
	}
	
	public void setCode(String code)
	{
		this.code=code;
	}
	
	public String toString()
	{
		return "registeredCode='"+code+"',updateStatus="+updateStatus+",orgName='"+orgName+"',province='"+provName+"',lastUpdateTime=getDate()";
	}
	
	public String getCols()
	{
		return "registeredCode,updateStatus,orgName,province,lastUpdateTime";
	}
	
	public String getVals()
	{
		return "'"+code+"',"+updateStatus+",'"+orgName+"','"+provName+"',getDate()";
	}
}
