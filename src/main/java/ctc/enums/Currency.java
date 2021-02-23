package ctc.enums;

/**
 * Currency:
 * Enumerates the cryptocurrencies and fiat currencies used
 */

public enum Currency {
    CAD, USD, BNB, BTC, BTG, ETH, LTC, XRP, BCC, BCH, TRX, XVG, XBT, XLM, REQ
    ,XEM,LOOM,TIX,XVC,EDG,DOGE,DNT,LBC,MAID,DASH,MLN,STR,WAVES,SDC,STEEM,DNET,
    DGB,DGD,ZRX,GUP,KMD,PASC,AMP,HKG,OMG,BLOCK,MYST,SJCX,BTA,VSL,DCR,FCT,TKN,NXC,
    ARK,STRAT,ENG,POWR,BTS,WINGS,XCP,RLC,GNO,GNT,SC,_1ST,VTR,PART,SNT,PLU,PIVX,
    FUN,LUN,MANA,SNGLS,UBQ,HMQ,XAUR,SALT,SWT,DAO,ETC,BAT,ICN,TRST,TAAS,BAY,REP,XMR,ADA,
    GAS,KNC,QTUM,AE,AST,EOS,SUB,MKR,TheDAO,NxC,ARC,ST,Xaurum,EMV,Guppy,ETB,NDC,
    ETBS,RDN,PPT,AMPX,Unicorns,BSV;
	
	

	public static Currency lookup(String coin) 
	{
		if(Character.isDigit(coin.charAt(0))) 
		{
			coin = "_" + coin;
		}
		Currency currency = null;
		try
		{
			currency = Currency.valueOf(coin);
		}
		catch(IllegalArgumentException e) 
		{
			e.printStackTrace();
		}
		if(currency == null) System.err.println("NULL FOR COIN=" + coin);
		return synonymFilter(currency);
	}
	
    /**
     * isFiat   - Returns if a Currency is fiat or not
     * @return boolean
     */
    public boolean isFiat() {
        return (this == CAD || this == USD);
    }

    /**
     * synonymFilter    - Converts less commonly used crypto symbols
     *                    to more common ones used by the API
     * @param toFilter  - The Currency to convert
     * @return Currency
     */
    public static Currency synonymFilter(Currency toFilter) {
        if (toFilter == Currency.XBT) return Currency.BTC;
        else if (toFilter == Currency.BCC) return Currency.BCH;
        else if (toFilter == Currency.TheDAO) return Currency.DAO;
        else if (toFilter == Xaurum) return XAUR;
        else if (toFilter == Guppy) return GUP;
        
        return toFilter;
    }
}
