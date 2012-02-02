import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import xml.eventbroker.DynamicRegistration;
import xml.eventbroker.XMLEventBroker;


public class SpeedTest {
	
	public static XMLEventBroker broker;
	
	public static void main(String[] args) {
		broker = new XMLEventBroker();
		Class<? extends XMLEventBroker> clazz = broker.getClass();
		try {
			//Initialize the Broker
			broker.init();
			
			//Register the speed-statistics-app
			Field dynRegF = clazz.getDeclaredField("dynReg");
			dynRegF.setAccessible(true);
			DynamicRegistration dynReg = (DynamicRegistration) dynRegF.get(broker);
			byte[] regEvent = "<HTTPConnector streaming=\"False\" event=\"timed-event\" url='http://localhost:8080/speed-statistics/SpeedStatistics'/>".getBytes("UTF-8");
			dynReg.subscribe(new ByteInputStream(regEvent, regEvent.length), "XMLBroker/speed-test/1");
			
			Method pM = clazz.getDeclaredMethod("processXML", InputStream.class);
			pM.setAccessible(true);
			
			System.out.println("Press <Enter> to continue...");
			Scanner sc = new Scanner(System.in);
			sc.nextLine();
			
			long start = System.nanoTime();
			pM.invoke(broker, new EventTestStream(0x10000, true));
			System.out.println("Time for parsing the events at the brokers side: "+((System.nanoTime()-start)/1000000)+"ms");
			
			Field poolF = clazz.getDeclaredField("pool");
			poolF.setAccessible(true);
			ExecutorService pool = (ExecutorService) poolF.get(broker);
			
			pool.shutdown();
			if (!pool.awaitTermination(10, TimeUnit.MINUTES))
				pool.shutdownNow();
			System.out.println("Sending finished after: "+((System.nanoTime()-start)/1000000)+"ms");
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
