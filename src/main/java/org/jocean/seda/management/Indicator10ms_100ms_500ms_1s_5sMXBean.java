/**
 * 
 */
package org.jocean.seda.management;

import org.jocean.seda.management.annotation.Indicator;

/**
 * @author isdom
 *
 */
public interface Indicator10ms_100ms_500ms_1s_5sMXBean {

	@Indicator(key="<10ms")
	public	long	get1_lt10ms();
	
	@Indicator(key=">=10ms&&<100ms")
	public	long	get2_lt100ms();

	@Indicator(key=">=100ms&&<500ms")
	public	long	get3_lt500ms();
	
	@Indicator(key=">=500ms&&<1s")
	public	long	get4_lt1s();
	
	@Indicator(key=">=1s&&<5s")
	public	long	get5_lt5s();
	
	@Indicator(key=">=5s")
	public	long	get6_mt5s();
}
