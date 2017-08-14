package netty;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Bootstrap {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("applicationContext.xml");
	}

}
