package netty.zk;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import netty.model.Constant;

public class ServiceRegistry {

	private Log logger = LogFactory.getLog(ServiceRegistry.class);

	// countDownLatch连接到zk
	private CountDownLatch cdl = new CountDownLatch(1);
	private String registryAddress;

	public ServiceRegistry(String registryAddress) {
		super();
		this.registryAddress = registryAddress;
	}

	public void registry(String data) {
		if (data != null) {
			ZooKeeper zk = connect2Server();
			if (zk != null) {
				createRootNode(zk);
				createNode(zk, data);
			}
		}
	}

	private ZooKeeper connect2Server() {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, new Watcher() {

				@Override
				public void process(WatchedEvent event) {
					if (event.getState() == Event.KeeperState.SyncConnected) {
						// 连接到zk时进行countDown
						cdl.countDown();
					}
				}
			});
			cdl.await();
		} catch (Exception e) {
			logger.error("Connect to zk error", e);
		}
		return zk;
	}

	private void createNode(ZooKeeper zk, String data) {
		try {
			byte[] bytes = data.getBytes();
			String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			logger.info("create zookeeper node (" + path + " => " + data + ")");
		} catch (KeeperException | InterruptedException e) {
			logger.error("create zookeeper error", e);
		}
	}
	
	// 若根节点不存在则创建一个PERSISTENT的节点
	private void createRootNode(ZooKeeper zk){
        try {
            Stat s = zk.exists(Constant.ZK_REGISTRY_PATH, false);
            if (s == null) {
                zk.create(Constant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e){
        	logger.error("create createRootNode error", e);
        }
    }
}
