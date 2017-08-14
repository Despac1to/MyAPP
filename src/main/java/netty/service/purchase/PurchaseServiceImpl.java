package netty.service.purchase;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import netty.annotation.MyService;

@MyService(PurchaseServiceImpl.class)
public class PurchaseServiceImpl implements PurchaseService {

	private static final Log logger = LogFactory.getLog(PurchaseServiceImpl.class);
	private static final String SUCCESS = "Congratulation, purchase success!";
	private static final String FAILURE = "Sorry, sold out!";
	private static final String FINISH = "Process finish...";
	private static final String UNFINISH = "Process unfinish...";

	private Map<Long, Long> purchaseMap = new ConcurrentHashMap<>();
	private AtomicInteger stock = new AtomicInteger(10000);
	private ArrayBlockingQueue<Long> purchaseQueue = new ArrayBlockingQueue<>(10000);
	
	private ExecutorService processPool = Executors.newSingleThreadExecutor();

	@Override
	public String purchase(long skuId, long userId) {
		// 无库存或无效用户
		if (stock.get() == 0 || userId <= 0) {
			return FAILURE;
		}
		try {
			// 1s内无法加入队列(队列已满),则返回
			purchaseQueue.offer(userId, 1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Run out of the stock.");
			return FAILURE;
		}
		stock.getAndDecrement();
		purchaseMap.put(skuId, userId);
		if (purchaseQueue.remainingCapacity() == 0) {
			// 最后一个purchase线程提交command
			processPurchase(purchaseQueue, purchaseMap);
		}
		return SUCCESS;
	}

	private void processPurchase(ArrayBlockingQueue<Long> userIds, Map<Long, Long> userSkuMap) {
		Future<String> futureRes = processPool.submit(new ProcessT(userIds, userSkuMap));
		String res;
		try {
			res = futureRes.get(20, TimeUnit.SECONDS);
		} catch (Exception e) {
			res = UNFINISH;
		}
		logger.info(res);
		// 线程池关闭
		processPool.shutdown();
		
	}

	private class ProcessT implements Callable<String> {

		private ArrayBlockingQueue<Long> userIds;
		private Map<Long, Long> userSkuMap;
		public ProcessT(ArrayBlockingQueue<Long> userIds, Map<Long, Long> userSkuMap) {
			this.userIds = userIds;
			this.userSkuMap = userSkuMap;
		}

		@Override
		public String call(){
			Long userId = null;
			try {
				userId = userIds.poll(1, TimeUnit.SECONDS);
				while(userId != null && userId > 0){
					// 具体业务处理,这里做简单关联
					logger.info("We should attach " + userId + " with " + userSkuMap.get(userId));
					
					userId = userIds.poll(1, TimeUnit.SECONDS);
				}
			} catch (InterruptedException e) {
				logger.info("Process Finish");
			}
			return FINISH;
		}
	}
}
