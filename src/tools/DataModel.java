package tools;

public class DataModel {

	public int updateStatus=-1;
	
	
	public String code;
	/*
	 * updateStatus->
	 * {0�����벻����
	 * ,1��������ڣ���ѯ�������
	 * ,2���ύ��ѯ�����쳣
	 * ,3��ҳ����Ӧ��ʱ
	 * ,4�������л�ʧ��
	 * ,5����֤����֤ʧ��
	 * ,6�����������
	 * ,7��δ֪
	 * ,8����ҳģ�����}
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
