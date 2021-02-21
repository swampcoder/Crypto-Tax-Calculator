package ctc.calculator;

import ctc.enums.Currency;

public interface IPriceInterface {

	public Double getPriceInUSD(Currency currency, long time);
	
}
