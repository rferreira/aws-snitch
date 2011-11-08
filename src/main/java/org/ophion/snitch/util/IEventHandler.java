package org.ophion.snitch.util;

public interface IEventHandler {
	public void handle(Object eventSource, Object eventArgs);
}