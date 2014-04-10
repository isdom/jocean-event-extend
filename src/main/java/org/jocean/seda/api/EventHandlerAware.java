package org.jocean.seda.api;

public interface EventHandlerAware {
	public void setEventHandler(final EventHandler handler) throws Exception;
}
