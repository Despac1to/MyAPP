package netty.handler;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import netty.model.Request;
import netty.model.Response;

public class MyHandler extends SimpleChannelInboundHandler<Request> {
	private Log logger = LogFactory.getLog(MyHandler.class);
	private final Map<String, Object> serviceMap;

	public MyHandler(Map<String, Object> serviceMap) {
		this.serviceMap = serviceMap;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Request req) throws Exception {
		Response response = new Response();
		response.setRequestId(req.getRequestId());
		try {
			Object res = handle(req);
			response.setResult(res);
		} catch (Exception e) {
			logger.error(e);
			response.setErrorInfo(e.getMessage());
		}
		// write并flush到client,添加结束监听
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private Object handle(Request req) throws Exception {
		String className = req.getClassName();
		String methodName = req.getMethodName();
		Object serviceObj = serviceMap.get(className);
		Class<?> serviceClass = serviceObj.getClass();
		Class<?>[] parameterTypes = req.getParameterTypes();
		Object[] parameters = req.getParameters();
		// cglib方式创建class
		FastClass serviceFClass = FastClass.create(serviceClass);
		FastMethod serviceFMethod = serviceFClass.getMethod(methodName, parameterTypes);
		// service对象调用对应方法
		return serviceFMethod.invoke(serviceObj, parameters);
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error occur!", cause);
        ctx.close();
    }

}
