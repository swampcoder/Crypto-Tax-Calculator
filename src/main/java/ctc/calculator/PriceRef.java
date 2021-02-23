package ctc.calculator;

public class PriceRef {

	private static IPriceInterface REF = null;
	
	public static IPriceInterface get() 
	{
		return REF;
	}
	
	public static void setRef(IPriceInterface ref) 
	{
		REF = ref;
	}
}
