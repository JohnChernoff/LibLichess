package org.chernovia.lib.lichess;

import com.fasterxml.jackson.databind.JsonNode;

public interface GameStreamWatcher {
	public void newEvent(JsonNode data);
	public void gameFinished(); 
}
