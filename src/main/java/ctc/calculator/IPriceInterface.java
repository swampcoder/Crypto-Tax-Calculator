package ctc.calculator;

import ctc.enums.Currency;

public interface IPriceInterface {
	public Double getPrice(Currency major, Currency minor, long time, boolean queryIfNotFound);
	
	public void setPrice(Currency major, Currency minor, long time, double price);
}
