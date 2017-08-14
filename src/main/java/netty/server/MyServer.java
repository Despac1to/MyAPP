package netty.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import netty.annotation.MyService;
import netty.endecode.MyDecoder;
import netty.endecode.MyEncoder;
import netty.handler.MyHandler;
import netty.model.Request;
import netty.model.Response;
import netty.zk.ServiceRegistry;

public class MyServer implements ApplicationContextAware, InitializingBean {

	private Log logger = LogFactory.getLog(MyServer.class);

	private String registryAddress;
	private ServiceRegistry serviceRegistry;
	// service对应name与对象map
	private Map<String, Object> serviceMap = new HashMap<>();

	public MyServer(String registryAddress, ServiceRegistry serviceRegistry) {
		this.registryAddress = registryAddress;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// 获取所有注解service
		Map<String, Object> _serviceMap = applicationContext.getBeansWithAnnotation(MyService.class);
		if (MapUtils.isNotEmpty(_serviceMap)) {
			for (Object service : serviceMap.values()) {
				// service类名称
				String name = service.getClass().getAnnotation(MyService.class).value().getName();
				serviceMap.put(name, service);
			}
		}
	}

	// 在setApplicationContext完成后执行
	@Override
	public void afterPropertiesSet() throws Exception {
		// boss线程组
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		// worker线程组
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap sBootstrap = new ServerBootstrap();
			sBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel sc) throws Exception {
							sc.pipeline().addLast(new MyDecoder(Request.class)) // 消息解析
								.addLast(new MyEncoder(Response.class)) // 消息序列话
								.addLast(new MyHandler(serviceMap)); // 消息处理
						}
					}).option(ChannelOption.SO_BACKLOG, 128) // TCP连接
					.childOption(ChannelOption.SO_KEEPALIVE, true); // 保持连接

			String host = registryAddress.split(":")[0];
			int port = Integer.valueOf(registryAddress.split(":")[1]);

			// 绑定端口
			ChannelFuture channelFuture = sBootstrap.bind(host, port).sync();
			logger.info("NettyServer start at: " + port);

			if (serviceRegistry != null) {
				serviceRegistry.registry(registryAddress); // 服务地址注册
			}
			// 同步关闭
			channelFuture.channel().closeFuture().sync();
		} finally {
			// 线程组shutdown
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
