/**
 * 
 */
package org.jocean.seda.management.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author isdom
 *
 */
@Retention(RetentionPolicy.RUNTIME) 
public @interface IndicateInterface {
    public abstract Class<?> type();
}
