/**
 * 
 */
package org.jocean.seda.management.impl;

import org.apache.commons.collections.Transformer;
import org.jocean.seda.management.annotation.IndicateInterface;



/**
 * @author isdom
 *
 */
@IndicateInterface(type = org.jocean.seda.management.Indicator10ms_100ms_500ms_1s_5sMXBean.class )
public class MillisecondsToText10ms_100ms_500ms_1s_5s implements Transformer {
	/* (non-Javadoc)
	 * @see org.apache.commons.collections.Transformer#transform(java.lang.Object)
	 */
	public Object transform(final Object input) {
		if ( input instanceof Long) {
			long value = (Long)input;
			if ( value < 10L ) {
				return	"<10ms";
			}
			else if ( value < 100L ) {
				return	">=10ms&&<100ms";
			}
			else if ( value < 500L ) {
				return	">=100ms&&<500ms";
			}
			else if (value < 1000L ) {
				return	">=500ms&&<1s";
			}
			else if (value < 5000L ) {
				return	">=1s&&<5s";
			}
			else {
				return	">=5s";
			}
		}
		return	null;
	}

}
