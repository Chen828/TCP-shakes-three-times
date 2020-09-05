/*
 *  构造TCP报文段；
	实现TCP连接建立的三次握手过程；
	实现TCP连接拆除的过程；
	显示建立和拆除连接过程中的相关信息。
 */

package INetDesign;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;


//三四握手，四次挥手
//被动断连接最终版

public class Clienttest {

	static NetworkInterface[] devices = JpcapCaptor.getDeviceList();//存放网络的接口设备的列表
	static int deviceseq;
	InetAddress srcip;
	static InetAddress dstip;
	JpcapSender sender = null;//第一次发送
	static TCPPacket tcp;//发送的tcp请求，第一次握手
	public static JpcapCaptor jCaptor;//与指定设备连接的对象
	static long reMessageAck;//接受消息包ack序号,区分接受消息包和二次挥手包
	static long sgoodbyeAck;//二次挥手包ack序号,区分接受消息包和二次挥手包
	Scanner in=new Scanner(System.in);
		
	public Clienttest() {
		tcp=new TCPPacket(3030, 80, 2222, 0, false, false, false, false, false, false,false, false, 26666, 0);
		/*
		 * jpcap.packet.TCPPacket.TCPPacket(int src_port, int dst_port, long sequence, long ack_num, boolean urg, 
		 * boolean ack, boolean psh, boolean rst, 
		 * boolean syn, boolean fin, boolean rsv1, boolean rsv2, int window, int urgent)
		 */
		/*
		 *  SYN表示建立连接，
			FIN表示关闭连接，
			ACK表示响应，
			PSH表示有 DATA数据传输，
			RST表示连接重置。
		 */
	}
	
	//设备接口
	public void devicelist(){
		try {
             // 获取本机上的网络接口对象数组
			//NetworkInterface[] devices = JpcapCaptor.getDeviceList();
			System.out.println("网卡列表：");
			for (int i = 0; i < devices.length; i++) {
				jpcap.NetworkInterface nc = devices[i];
				// 一块卡上可能有多个地址:
				String address = "";
				for (int t = 0; t < nc.addresses.length; t++) {
					address += "  addresses[" + t + "]: "
							+ nc.addresses[t].address.toString();
				}
				// 打印说明:
				
				System.out.println("第" + i + "个接口:\n" + "name: " + nc.name
						+ "  loopback: " + nc.loopback + "\r\naddress: "
						+ address);
			}
		} catch (Exception ef) {
			ef.printStackTrace();
			System.out.println("显示网络接口数据失败:  " + ef);
		}
	}	
	
	//得IP地址、发送
	public void getsrc(){
		System.out.println("\n请输入网卡设备序号：");
		deviceseq=in.nextInt();
		String ip=devices[deviceseq].addresses[1].address.toString().substring(1 );//substring(strat,stop)
		String[] ipstr=ip.split("\\.");//以·拆分字符串为四组
		byte[] ipbyte=new byte[4];
		for(int i=0;i<4;i++){
			ipbyte[i]=(byte)(Integer.parseInt(ipstr[i]));//强制类型转换
			//点分十进制，解决出现负数问题，system以补码存储
			int buma=ipbyte[i];
			if(buma<0){
				buma+=256;
			}
		}
		try {
			srcip=InetAddress.getByAddress(ipbyte);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	public void getdst(){
		System.out.println("请输入目的网站：（格式为http://www.baidu.com）");
		try {
			URL url = new URL(in.next());//输入的url字段
			String host=url.getHost();//返回url主机名
			InetAddress address=null;
			address=InetAddress.getByName(host);//根据主机名返回对象
			if(host.equalsIgnoreCase(address.getHostAddress())){
				//如果host与解析结果相等，说明host为IP
				//因为host是IP地址，所以getByName(host)不能获得完整的address对象，其中只有ip
				String[] ipstr=host.split("\\.");//以·拆分字符串为四组
				byte[] ipbyte=new byte[4];
				for(int i=0;i<4;i++){
					ipbyte[i]=(byte)(Integer.parseInt(ipstr[i]));//强制类型转换
					//点分十进制，解决出现负数问题，system以补码存储
					int buma=ipbyte[i];
					if(buma<0){
						buma+=256;
					}
					System.out.print(buma+".");
				}
				dstip=InetAddress.getByAddress(ipbyte);
			}else if(host.equalsIgnoreCase(address.getHostName())){	
				dstip=InetAddress.getByName(host);
			}			
		} catch (MalformedURLException e) {
			System.out.println("您的输入URL协议、格式或者路径错误！");
		} catch (UnknownHostException e) {
			System.out.println("您的输入并非有效IP地址/域名！");
		}	
	}
	public void sendip_ether(){
		try {
			sender = JpcapSender.openDevice(devices[deviceseq]);
		} catch (IOException e) {
			e.printStackTrace();
		}		
		tcp.setIPv4Parameter(0, false, false, false, 0, false , true, false, 0,1010101, 100, IPPacket.IPPROTO_TCP, 
				srcip,dstip);
		/*
		 * void jpcap.packet.IPPacket.setIPv4Parameter(int priority, boolean d_flag, boolean t_flag, boolean r_flag, 
		 * int rsv_tos, boolean rsv_frag,
		 *  boolean dont_frag ,？？？？？？？
		 *  boolean more_frag, int offset, int ident, int ttl, int protocol, 
		 * InetAddress src, InetAddress dst)
		 */
		EthernetPacket ether = new EthernetPacket();
		ether.frametype = EthernetPacket.ETHERTYPE_IP;
		ether.src_mac = new byte[] { (byte) 0xa0, (byte) 0xa8, (byte) 0xcd, (byte) 0xb9,
			    (byte) 0x41, (byte) 0x23 };//本机MAC
		
		//byte[] mac = NetworkInterface.getByInetAddress(srcip).getHardwareAddress();
		ether.dst_mac = new byte[] { (byte) 0x58, (byte) 0x69, (byte) 0x6c, (byte) 0x5f,
			    (byte) 0x50, (byte) 0x9e };//同一台锐捷MAC
		tcp.datalink = ether;
		sender.sendPacket(tcp);
		sender.close();
		//tcp.setIPv4Parameter.ident++;
	}
	
	//第一次握手
	public  void fhandshake() throws IOException{		   
		tcp.syn=true;		
		tcp.data = ("").getBytes();
		sendip_ether();
		System.out.println("第一次握手:\n");
		system();
	}
	
	//第二次握手
	public void shandshake() throws IOException  {
		//接收第二次握手
		jCaptor=JpcapCaptor.openDevice(devices[deviceseq], 65535, false, 2000);		
		//打开网卡:JpcapCaptor jpcap.JpcapCaptor.openDevice(NetworkInterface intrface,
		//int snaplen,每个数据包中截获的前多少字节，65535为全部
		//boolean promisc,混杂模式，开启可接受所有帧，关闭只接收和本机MAC地址一致的帧
		//int to_ms)用于processpacket（）方法，指定超时的时间
		jCaptor.loopPacket(-1, new packetReceiver2());
		/*
		 * 	while(flag){
				Packet packet=jCaptor.getPacket();//获取数据包另一方法
			}
		 */
		
	}
	
	//第三次握手
	public  void thandshake() throws IOException{
		//第一次中TCPPacket(3030, 80, 33333, 0, false, false, false, false, true, false,false, false, 666, 0);
		tcp.syn=false;
		tcp.sequence=packetReceiver2.tcpPacket.ack_num;//静态 //第二次握手的ack_num
		reMessageAck=tcp.sequence+38;//af-d8=41,因为ack为请求的下一位序号
		tcp.ack=true;
		tcp.ack_num=packetReceiver2.tcpPacket.sequence+1;	
		tcp.data = ("GET / HTTP/1.1"+"\r\n"+"Host: www.baidu.com"+"\r\n"+"\n").getBytes();
		sendip_ether();
		System.out.println("第三次握手:\n");
		system();
				//new String(tcp3.data,"utf-8")用法英文可输出，中文不识别
	}
	
	//接收消息
	public void remessage(){
		try {
			jCaptor=JpcapCaptor.openDevice(devices[deviceseq], 65535, false, 2000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//jCaptor.loopPacket(-1, new packetReceiver2());//ack
		//jCaptor.loopPacket(-1, new packetReceiver2());//ack+http
		while (packetReceiver2.tcpPacket.ack&&packetReceiver2.tcpPacket.fin==false) {
			//packetReceiver2.tcpPacket.psh&&packetReceiver2.tcpPacket.fin==false
			jCaptor.loopPacket(-1, new packetReceiver2());			
		}
	}

	//连接拆除
	
	private void secondg() {
		tcp.fin=false;
		tcp.ack=true;
		tcp.sequence=packetReceiver2.tcpPacket.ack_num;//消息的ack_num
		tcp.ack_num=packetReceiver2.tcpPacket.sequence+1;
		tcp.data = ("").getBytes();
		sendip_ether();
		System.out.println("第二次挥手:\n");
		system();		
	}	
	
	private void thirdg() {
		tcp.fin=true;
		tcp.ack=true;
		tcp.sequence=packetReceiver2.tcpPacket.ack_num;//消息的ack_num
		//tcp.ack_num=packetReceiver2.tcpPacket.sequence+1;
		tcp.data = ("").getBytes();
		sendip_ether();
		System.out.println("第三次挥手:\n");
		system();		
	}

	private void fourg() {
		try {
			jCaptor=JpcapCaptor.openDevice(devices[deviceseq], 65535, false, 2000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		jCaptor.loopPacket(-1, new packetReceiver2());
		
	}
	
	//打印Packet
	public void system(){
		try {
			System.out.println("Src_IP：" +tcp.src_ip+ "  Dst_IP："+tcp.dst_ip.getHostAddress()
					+ "  Src_port：" + tcp.src_port+ "  Dst_port：" + tcp.dst_port +"\n"
					+"SYN:"+tcp.syn+"  FIN:"+tcp.fin+"  ACK:"+tcp.ack +"\n"
					+"Sequence："+tcp.sequence+"  Ack_Num:"+tcp.ack_num + "  Protocol：" + tcp.protocol+"\n"
					+"Data："+new String(tcp.data,"utf-8")+"\n");//new String此用法英文可输出，中文不识别
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException{
		Clienttest cl=new Clienttest();
		cl.devicelist();
		//连接建立
		cl.getsrc();
		cl.getdst();
		cl.fhandshake(); 
		cl.shandshake();
		cl.thandshake();
		cl.remessage();
		//连接拆除
		cl.secondg();
		cl.thirdg();
		cl.fourg();
	}

}
class packetReceiver2 implements PacketReceiver{
	public static TCPPacket tcpPacket;
	public void receivePacket(Packet packet) {
		//若是没导入Packet包（import），则可直接“jpcap.packet.Packet packet”
		if (packet instanceof TCPPacket) {
			tcpPacket = (TCPPacket) packet;//接收到的tcp数据包
			if(tcpPacket.src_ip.getHostAddress().equalsIgnoreCase(Clienttest.dstip.getHostAddress())
					&&tcpPacket.dst_port==3030&&tcpPacket.src_port==Clienttest.tcp.dst_port){
				//确保符合条件与包一一对应
				if(tcpPacket.syn==true&&tcpPacket.fin==false&&tcpPacket.ack==true
						&&tcpPacket.ack_num==Clienttest.tcp.sequence+1){
					//第二次握手
					System.out.println("第二次握手：\n");
					system();
				}else if(tcpPacket.syn==false&&tcpPacket.fin==false&&tcpPacket.ack==true
						&&tcpPacket.ack_num==Clienttest.reMessageAck){//&&tcpPacket.data.equals("")
					//接收消息
					System.out.println("接受的消息：\n");
					system();
				}else if (tcpPacket.syn==false&&tcpPacket.fin==true&&tcpPacket.ack==true
						&&tcpPacket.ack_num==Clienttest.reMessageAck) {//&&tcpPacket.ack_num==Clienttest.reMessageAck
					System.out.println("第一次挥手：\n");
					system();
				}else if (tcpPacket.syn==false&&tcpPacket.fin==false&&tcpPacket.ack==true
						&&tcpPacket.sequence==Clienttest.tcp.ack_num) {//&&tcpPacket.sequence==Clienttest.tcp.ack_num
					System.out.println("第四次挥手：\n");
					system();
				}
			}
		}
	}
	public void system(){
		try {
			System.out.println("  Src_IP：" +tcpPacket.src_ip+ "  Dst_IP："+tcpPacket.dst_ip
					+ "  Src_port：" + tcpPacket.src_port+ "  Dst_port：" + tcpPacket.dst_port +"\n"
					+"  SYN:"+tcpPacket.syn+"  FIN:"+tcpPacket.fin+"  ACK:"+tcpPacket.ack +"\n"
					+"  Sequence："+tcpPacket.sequence+"  Ack_Num:"+tcpPacket.ack_num + "  Protocol：" + tcpPacket.protocol+"\n"
					+"  Data："+"\n"+new String(tcpPacket.data,"utf-8")+"\n");
			Clienttest.jCaptor.breakLoop();
			//breakloop()要求静态调用
			//调用其他类中的成员，静态直接调用变量“类名.变量名”
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
