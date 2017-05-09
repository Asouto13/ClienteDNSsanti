
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import es.uvigo.det.ro.simpledns.*;

public class dnsclient {
	static String tipo_recurso = "";
	static String[] linea;
	static byte[] bytes= new byte[16];
	static String nombre_recurso = "";
	static String tipo_recurso_deco="";
	static InetAddress DNSraiz,DNS;
	static Inet6Address DNS6;
	static Inet6Address DNSraiz6;
	static DomainName nombre;
	static Scanner scanner;
	static Boolean error = false,fin = false;
	static RRType tipo;
	static AResourceRecord a;
	static RRType ns;
	static ResourceRecord rr;
	static int contador = 0;
	static File archivo= new File("archivo.txt");
	public static void main(String[] args) throws Exception{
		Message mensaje = null;
		System.out.println("Introduzca tipo de busqueda y nombre:");
		Scanner s = new Scanner(System.in);

		
		while(s.hasNextLine()){
		System.out.println("-----------------------");
		linea= s.nextLine().split("\\s+");
		
		String protocolo = args[0];
		tipo_recurso= linea[0].trim();//pregunta.split(" ")[0];
		nombre_recurso= linea[1].trim();//pregunta.split(" ")[1];
		
		DNSraiz =InetAddress.getByName(args[1]);
		DNS = DNSraiz;
		error = false;
		
		while(!fin){
		switch(tipo_recurso){
		
		case "A":			
			mensaje = new Message(nombre_recurso,RRType.A,false);
			tipo_recurso_deco = "A";
			break;
			
		case "NS":
			mensaje = new Message(nombre_recurso,RRType.NS,false);
			tipo_recurso_deco = "NS";
			break;
			
		case "AAAA":
			mensaje = new Message(nombre_recurso,RRType.AAAA,false);
			tipo_recurso_deco = "AAAA";
			break;
		default:
			System.out.println("parametro no valido");
			error = true;
			break;
		}
		if(error){
			break;
		}else if(protocolo.equals("-t")){	
			System.out.println("Q: TCP "+DNS.getHostAddress()+" "+mensaje.getQuestionType().toString()+" "+nombre_recurso);
			ConexionTCP(DNS,mensaje.toByteArray(),53);
		}else if(protocolo.equals("-u")){	
			System.out.println("Q: UDP "+DNS.getHostAddress()+" "+mensaje.getQuestionType().toString()+" "+nombre_recurso);
			ConexionUDP(DNS,mensaje.toByteArray(),53);
		}	
		}
		System.out.println("\nIntroduzca tipo de busqueda y nombre:");		
		if(s.hasNextLine())
			fin=false;
		}
		s.close();
	}
	
	private static void ConexionUDP(InetAddress DNSdir,byte mensaje[],int port) throws Exception{
		DatagramSocket socket;
		DatagramPacket paquete,recibo;
		byte[] datos;
		String texto;
		DataInputStream entrada;
		DataOutputStream salida;
		Message respuesta;
		List<ResourceRecord> answers;
			paquete = new DatagramPacket(mensaje,mensaje.length,(Inet4Address)DNSdir,53);
			socket = new DatagramSocket();
			
			datos = new byte[1000];
			recibo = new DatagramPacket(datos,datos.length);
			
			socket.send(paquete);
			//System.out.println("paquete enviado a "+DNSdir+", esperando respuesta...");
			socket.receive(recibo);
			respuesta = new Message(recibo.getData());
			decodificadorUDP(respuesta);
			socket.close();
			//scanner.close();		
	}


	private static void ConexionTCP(InetAddress DNSdir,byte mensaje[],int port) throws Exception{
		Socket socket;
		DataOutputStream salida;
		DataInputStream entrada;
		Message respuesta;
		byte[] respuesta_bytes = new byte[1000];
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		outputStream.write(Utils.int16toByteArray(mensaje.length));
		outputStream.write(mensaje);
		byte mensajeTCP[] = outputStream.toByteArray( );
		
		
		socket = new Socket(DNSdir,53);
		
		salida = new DataOutputStream(socket.getOutputStream());
		entrada = new DataInputStream(socket.getInputStream());
		
		salida.write(mensajeTCP);
		
		entrada.read(respuesta_bytes);
		respuesta_bytes = Arrays.copyOfRange(respuesta_bytes, 2, respuesta_bytes.length);
		respuesta = new Message(respuesta_bytes);
		decodificadorTCP(respuesta);
		
	}
	
	private static void decodificadorUDP(Message respuesta) throws IOException, Exception{
		String texto;
		List<ResourceRecord> answers, name;
		NSResourceRecord NS,NSutil = null;
		int o = 0;
		boolean igual = false;
		texto = String.format("%s.",nombre_recurso);
			//System.out.println("Tama�o AdditonalRecords: "+respuesta.getAdditonalRecords().size());
			if(respuesta.getAnswers().isEmpty() && !respuesta.getAdditonalRecords().isEmpty()){
				//System.out.println("estamos 178");
				name = respuesta.getNameServers();
				answers = respuesta.getAdditonalRecords();
				//COMPROBACIONES
				//System.out.println("AdditionalRecords("+answers.size()+"):");
				for(int i = 0 ; i < answers.size();i++){
				//	System.out.println(answers.get(i).getDomain().toString());
				}
				//System.out.println("NameDomain:");
				for(int z = 0 ; z < name.size();z++){
					NS = (NSResourceRecord)name.get(z);
				//	System.out.println(NS.getNS().toString());
				}
				while(!igual){		
					for(int i = 0; i< answers.size();i++){
						NS = (NSResourceRecord)name.get(i);
						if(NS.getNS().toString().equals(answers.get(o).getDomain().toString())){
							//System.out.println("igual: "+NS.getNS().toString()+" "+answers.get(o).getDomain().toString());							
							igual = true;
							NSutil = NS;
							break;
						}
					};
					o++;
				}
				igual = false;
				o = 0;
				
				//System.out.println("tipo mensaje: "+answers.get(0).getRRType().toString());
				Message nuevo = new Message(NSutil.getNS().toString(),answers.get(0).getRRType(),false);
				
				switch(tipo_recurso_deco){
				case "NS":
				case "A":
					AResourceRecord a = (AResourceRecord)answers.get(0);
					ConexionUDP(a.getAddress(),nuevo.toByteArray(),53);		
					break;
				case "AAAA":
				//	System.out.println("entr� en AAAA");
					AResourceRecord aaaa= (AResourceRecord)answers.get(0);
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getTTL()+" "+(aaaa).getAddress().getHostAddress());
					ConexionUDP(aaaa.getAddress(),nuevo.toByteArray(),53);
					break;
				case "CNAME":
					break;
				}
				
			}else if(!respuesta.getAnswers().isEmpty() ){
				//System.out.println("estamos 225");
				answers = respuesta.getAnswers();
				switch(tipo_recurso_deco){
				case "NS":
					AResourceRecord ax = (AResourceRecord)answers.get(0);
					for(int i=0;i<respuesta.getNameServers().size();i++){
					NSResourceRecord NSR = (NSResourceRecord)respuesta.getNameServers().get(i);
					if(!NSR.getNS().toString().equals(ax.getDomain().toString().trim())){
					//System.out.println(ax.getDomain().toString());
					//System.out.println(NSR.getNS().toString());
					System.out.println("A: "+ax.getAddress().getHostAddress()+" "+tipo_recurso+" "+NSR.getNS().toString());
					DNS=ax.getAddress();
					}else{
						fin=true;
						System.out.println("A: "+ax.getAddress().getHostAddress()+" "+tipo_recurso+" "+NSR.getNS().toString());
					}
					}
					break;
				case "A":
					AResourceRecord a = (AResourceRecord)answers.get(0);
					if(!texto.equals(a.getDomain().toString().trim())){
						DNS = a.getAddress();	
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getDomain().toString());
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getTTL()+" "+a.getAddress().toString().replace("/",""));
					}
					else{	
							
						fin = true;
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getTTL()+" "+a.getAddress().toString().replace("/",""));
					}
					break;
					
				
				case "AAAA":
				//	System.out.println("entr� en AAAA");
					AResourceRecord aaaa= (AResourceRecord)answers.get(0);
					if(!texto.equals(aaaa.getDomain().toString().trim())){
						DNS = aaaa.getAddress();
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getDomain().toString());
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getTTL()+" "+aaaa.getAddress().toString().replace("/",""));
					}
					else{						
						fin = true;
						System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getTTL()+" "+aaaa.getAddress().toString().replace("/",""));
					}
					break;
				}
				
			}else{
				name = respuesta.getNameServers();
			//	System.out.println("nombres dominio");
				for(int i = 0; i < name.size();i++){
			//		System.out.println(name.get(i).getDomain().toString());
				}
				//System.out.println("estamos 262");
				if(name.isEmpty()){
					
					System.out.println("A: "+DNS.getHostAddress()+" CNAME");
				}
				else{
					System.out.println("No hay respuestas.");
				}
				//System.out.println("No hay respuesta");
				fin = true;
			}
		
	}
	private static void decodificadorTCP(Message respuesta) throws IOException, Exception{
		String texto;
		List<ResourceRecord> answers, name;
		NSResourceRecord NS,NSutil = null;
		int o = 0;
		boolean igual = false;
		texto = String.format("%s.",nombre_recurso);
		if(respuesta.getAnswers().isEmpty() && !respuesta.getAdditonalRecords().isEmpty()){
			answers = respuesta.getAdditonalRecords();
			Message nuevo = new Message(answers.get(0).getDomain().toString(),answers.get(0).getRRType(),false);
			switch(tipo_recurso_deco){
			case "NS":
			case "A":
			//	System.out.println("entr� en A");
				AResourceRecord a = (AResourceRecord)answers.get(0);
				ConexionTCP(a.getAddress(),nuevo.toByteArray(),53);		
				break;
			
			case "AAAA":
		// 		System.out.println("entr� en AAAA");
				AAAAResourceRecord aaaa= (AAAAResourceRecord)answers.get(0);
				break;
			}		
		}else if(!respuesta.getAnswers().isEmpty() ){
			answers = respuesta.getAnswers();
			switch(tipo_recurso_deco){
			case "NS":
				AResourceRecord ax = (AResourceRecord)answers.get(0);
				for(int i=0;i<respuesta.getNameServers().size();i++){
				NSResourceRecord NSR = (NSResourceRecord)respuesta.getNameServers().get(i);
				if(!NSR.getNS().toString().equals(ax.getDomain().toString().trim())){
				//System.out.println(ax.getDomain().toString());
				//System.out.println(NSR.getNS().toString());
				System.out.println("A: "+ax.getAddress().getHostAddress()+" "+tipo_recurso+" "+NSR.getNS().toString());
				DNS=ax.getAddress();
				}else{
					fin=true;
					System.out.println("A: "+ax.getAddress().getHostAddress()+" "+tipo_recurso+" "+NSR.getNS().toString());
				}
				}
				break;
			case "A":
				AResourceRecord a = (AResourceRecord)answers.get(0);
				if(!texto.equals(a.getDomain().toString().trim())){
					DNS = a.getAddress();	
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getDomain().toString());
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getTTL()+" "+a.getAddress().toString().replace("/",""));
				}
				else{	
						
					fin = true;
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+a.getTTL()+" "+a.getAddress().toString().replace("/",""));
				}
				break;
				
			
			case "AAAA":
//				System.out.println("entr� en AAAA");
				AResourceRecord aaaa= (AResourceRecord)answers.get(0);
				if(!texto.equals(aaaa.getDomain().toString().trim())){
					DNS = aaaa.getAddress();
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getDomain().toString());
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getTTL()+" "+aaaa.getAddress().toString().replace("/",""));
				}
				else{						
					fin = true;
					System.out.println("A: "+DNS.getHostAddress()+" "+answers.get(0).getRRType().toString()+" "+aaaa.getTTL()+" "+aaaa.getAddress().toString().replace("/",""));
				}
				break;
			}
			
		}else{
			name = respuesta.getNameServers();
			//	System.out.println("nombres dominio");
				for(int i = 0; i < name.size();i++){
			//		System.out.println(name.get(i).getDomain().toString());
				}
				//System.out.println("estamos 262");
				if(name.isEmpty()){
					
					System.out.println("A: "+DNS.getHostAddress()+" CNAME");
				}
				else{
					System.out.println("No hay respuestas.");
				}
				//System.out.println("No hay respuesta");
				fin = true;
		}
	}
}

