/**
 * 
 */
package org.jocean.event.extend.management.impl;

import javax.annotation.Nullable;

import org.jocean.event.extend.management.annotation.IndicateInterface;

import com.google.common.base.Function;

/**
 * @author isdom
 *
 */
@IndicateInterface(type = org.jocean.event.extend.management.Indicator10ms_100ms_500ms_1s_5sMXBean.class )
public class MillisecondsToText10ms_100ms_500ms_1s_5s 
    implements Function<Long, String> {
    public @Nullable String apply(@Nullable Long input) {
        final long value = input;
        if (value < 10L) {
            return "<10ms";
        } else if (value < 100L) {
            return ">=10ms&&<100ms";
        } else if (value < 500L) {
            return ">=100ms&&<500ms";
        } else if (value < 1000L) {
            return ">=500ms&&<1s";
        } else if (value < 5000L) {
            return ">=1s&&<5s";
        } else {
            return ">=5s";
        }
    }
}
