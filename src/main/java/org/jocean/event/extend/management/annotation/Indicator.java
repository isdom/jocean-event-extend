/**
 * 
 */
package org.jocean.event.extend.management.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author isdom
 *
 */
@Retention(RetentionPolicy.RUNTIME) 
public @interface Indicator {
    public abstract String key();
}
