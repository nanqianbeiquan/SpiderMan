package nacao;

import java.io.IOException;

import tools.DataModel;
public class NACAO extends DataModel{

	public String orgName=null; //机构名称
	public String orgType=null; //机构类型
	public String validPeriod=null; //有效期
	public String issuingAuthority=null; //颁发单位
	public String registeredCode=null; //机构登记证号
	public String registeredAddress=null; //地址
	public String reservea=null; //社会统一信用代码
	public int certificateExists=-1; //机构证书是否存在，存在：1，不存在：0
	/*
	 * updateStatus->{0：无此机构代码，1：正常，2：提交查询请求异常，
	 * 3：页面响应超时，4：您所在的IP暂时不能进行检索，7：未知}
	 */
	
	public NACAO(String code)
	{
		super(code);
	}
	
//	public NACAO(String code,float updateStatus)
//	{
//		this.code=code;
//		this.updateStatus=updateStatus;
//	}
	
	public void setUpdateStatus(int updateStatus)
	{
		this.updateStatus=updateStatus;
	}
	
	public void setOrgName(String orgName)
	{
		this.orgName=orgName;
	}
	public void setOrgType(String orgType)
	{
		this.orgType=orgType;
	}
	public void setValidPeriod(String validPeriod)
	{
		this.validPeriod=validPeriod;
	}
	public void setIssuingAuthority(String issuingAuthority)
	{
		this.issuingAuthority=issuingAuthority;
	}
	public void setRegisteredCode(String registeredCode)
	{
		this.registeredCode=registeredCode;
	}
	public void setRegisteredAddress(String registeredAddress)
	{
		this.registeredAddress=registeredAddress;
	}
	public void setCertificateExists(int certificateExists)
	{
		this.certificateExists=certificateExists;
	}
	public void setReservea(String reservea)
	{
		this.reservea=reservea;
	}
	public String toString()
	{
//		StringBuilder res=new StringBuilder("orgCode='"+code+"',");
		StringBuilder res=new StringBuilder("code='"+code+"',");
		if(orgName!=null)
		{
			res.append("registered_name='"+orgName+"',");
		}
		if(orgType!=null)
		{
			res.append("org_Type='"+orgType+"',");
		}
		if(validPeriod!=null)
		{
			res.append("valid_Period='"+validPeriod+"',");
		}
		if(issuingAuthority!=null)
		{
			res.append("issuing_Authority='"+issuingAuthority+"',");
		}
		if(registeredCode!=null)
		{
			res.append("registered_number='"+registeredCode+"',");
		}
		if(registeredAddress!=null)
		{
			res.append("registered_Address='"+registeredAddress+"',");
		}
//		if(certificateExists!=-1)
//		{
//			res.append("certificateExists="+certificateExists+",");
//		}
//		if(reservea!=null)
//		{
//			res.append("reservea='"+reservea+"',");
//		}
		res.append("updateStatus='"+updateStatus+"'");
		return res.toString();
	}
	
	public String[] getColsAndVals()
	{
//		StringBuilder cols=new StringBuilder("orgCode,");
		StringBuilder cols=new StringBuilder("code,");
		StringBuilder vals=new StringBuilder("'"+code+"',");
		
		if(orgName!=null)
		{
			cols.append("registered_name,");
//			cols.append("orgName,");
			vals.append("'"+orgName+"',");
		}
		if(orgType!=null)
		{
			cols.append("org_Type,");
//			cols.append("orgType,");
			vals.append("'"+orgType+"',");
		}
		if(validPeriod!=null)
		{
//			cols.append("validPeriod,");
			cols.append("valid_Period,");
			vals.append("'"+validPeriod+"',");
		}
		if(issuingAuthority!=null)
		{
			cols.append("issuing_Authority,");
//			cols.append("issuingAuthority,");
			vals.append("'"+issuingAuthority+"',");
		}
		if(registeredCode!=null)
		{
			cols.append("registered_number,");
//			cols.append("registeredCode,");
			vals.append("'"+registeredCode+"',");
		}
		if(registeredAddress!=null)
		{
			cols.append("registered_Address,");
//			cols.append("registeredAddress,");
			vals.append("'"+registeredAddress+"',");
		}
//		if(certificateExists!=-1)
//		{
//			cols.append("certificateExists,");
//			vals.append(certificateExists+",");
//		}
//		if(reservea!=null)
//		{
//			cols.append("reservea,");
//			vals.append("'"+reservea+"',");
//		}
//		cols.append("extract_status");
		cols.append("updateStatus");
		vals.append(updateStatus);
		
		return new String[]{cols.toString(),vals.toString()};
	}
	
	public static String generateCode(int baseCode)
	{
		return generateCode(String.format("%08d",baseCode));
	}
	public static String generateCode(String baseCode)
	{
		double[] weight={3,7,9,10,5,8,4,2};
		char checkCode;
		int s=0;
		for(int i=0;i<8;i++)
		{
			int c=(int)baseCode.charAt(i);
			if(c>=48 && c<58) c-=48;
			else c-=55;
			s+=weight[i]*c;
		}
		int c9=11-(s%11);
		if(c9==10)
			checkCode='X';
		else if(c9==11)
			checkCode='0';
		else
			checkCode=(char)(c9+48);
		return baseCode+checkCode;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
//		NACAO nacao=new NACAO("000000019","济南高新技术产业开发区管委会经贸局技术监督处",0,1);
//		String[] colsAndVals=nacao.getColsAndVals();
//		String cols=colsAndVals[0]+",certificateSavePath,lastUpdateTime";
//		
//		String vals=colsAndVals[1]+",getDate()";
//		String insertSql=String.format("insert into %s(%s) values(%s)","BaiduApp",cols,vals);
		System.out.println(generateCode(66000000));
	}
}
