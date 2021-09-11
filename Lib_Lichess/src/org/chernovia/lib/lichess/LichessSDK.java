package org.chernovia.lib.lichess;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LichessSDK {
	
	public static class GameStreamer extends Thread {
		GameStreamWatcher watcher;
		String gid;
		boolean running = false;
		
		public GameStreamer(String id, GameStreamWatcher gsw) {
			gid = id; watcher = gsw;
		}
		
		public void run() {
			running = true;
			BufferedReader reader = new BufferedReader(new InputStreamReader(streamGame(gid)));
			while (running) {
				try { 
					String line = reader.readLine();
					if (line == null) {
						watcher.gameFinished(); running = false;
					}
					else watcher.newEvent(mapper.readTree(line));
				}
				catch (IOException ergh) { ergh.printStackTrace(); return; }
			}
		}
	}
	
	static final Logger logger = Logger.getLogger("LichessSDK_Log");
	static final ObjectMapper mapper = new ObjectMapper();
		
	public static JsonNode apiRequest(String endpoint, String oauth) { return apiRequest(endpoint,oauth,true,null);	}
	public static JsonNode apiRequest(String endpoint, String oauth, boolean get, String body) {
		try { 
			Connection conn = Jsoup.connect("https://lichess.org/api/" + endpoint).
					ignoreContentType(true).
					header("Accept","application/json").
					header("Authorization","Bearer " + oauth);
			if (body != null) conn = conn.requestBody(body);
			Document doc = get ? conn.get() : conn.post();
			return mapper.readTree(doc.body().text());  
		}
		catch (Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JsonNode createGame(String username, String blackOauth, String whiteOauth ) { 
		return createGame(false,0,0,1,username, blackOauth,whiteOauth);
	}
	public static JsonNode createGame(boolean rated, int clock, int inc, int days, String username, 
			String blackOauth, String whiteOauth) {
		JsonNode response = null;
		try { 
			String queryString = "rated=" + (rated ? "true" : "false");
			if (days > 0) queryString += "&days=" + days;
			else {
				queryString += "&clock.limit=" + clock;
				queryString += "&clock.increment=" + inc;
			}
			queryString += "&color=white";
			queryString += "&acceptByToken=" + blackOauth;
			response = mapper.readTree(Jsoup.connect("https://lichess.org/api/challenge/" + username).
				ignoreContentType(true).
				header("Authorization","Bearer " + whiteOauth).
				requestBody(queryString).
				post().body().text());
		}
		catch (Exception e) { e.printStackTrace(); }
		return response;
	}
	
	public static BufferedInputStream streamGame(String gid) {
		try { 
			return Jsoup.connect("https://lichess.org/api/stream/game/" + gid).
				timeout(0).	
				header("Accept","application/x-ndjson").
				ignoreContentType(true).
				method(Connection.Method.GET).
				execute().bodyStream();  
		}
		catch (Exception e) { e.printStackTrace(); return null; }
	}
	
	public static BufferedInputStream streamGame(String[] players) {
		String playStr = players[0]; for (int i=1;i<players.length;i++) playStr += ("," + players[i]); 
		try { 
			return Jsoup.connect("https://lichess.org/api/stream/games-by-users").
				timeout(0).	
				header("Accept","application/x-ndjson").
				ignoreContentType(true).
				requestBody(playStr).
				method(Connection.Method.POST).
				execute().bodyStream();  
		}
		catch (Exception e) { e.printStackTrace(); return null; }
	}
	
	public static Iterator<JsonNode> getGames(String oauth) {
		try { 
			return mapper.readTree(Jsoup.connect("https://lichess.org/api/account/playing").
				ignoreContentType(true).
				header("Accept","application/json").
				header("Authorization","Bearer " + oauth).
				//validateTLSCertificates(false).
				get().body().text()).get("nowPlaying").elements();  
		}
		catch (Exception e) { e.printStackTrace(); return null; }
	}
}
