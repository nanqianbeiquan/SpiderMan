package tools;

public class DataModel {

	public int updateStatus=-1;
	
	
	public String code;
	/*
	 * updateStatus->
	 * {0：代码不存在
	 * ,1：代码存在，查询结果正常
	 * ,2：提交查询请求异常
	 * ,3：页面响应超时
	 * ,4：窗口切换失败
	 * ,5：验证码验证失败
	 * ,6：浏览器崩溃
	 * ,7：未知
	 * ,8：网页模板错误}
	 */
	
	public DataModel(String code)
	{
		this.code=code;
	}
	
	
	
	public void setUpdateStatus(int updateStatus)
	{
		this.updateStatus=updateStatus;
	}
}
