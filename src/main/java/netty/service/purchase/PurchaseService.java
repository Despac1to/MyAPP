package netty.service.purchase;
 
public interface PurchaseService {
	
	String purchase(long skuId, long userId);
}
