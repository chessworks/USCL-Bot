package org.chessworks.uscl.services.file;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.util.FileHelper;

public class TournamentService {

	private String fileName;
	private Map<String, Player> players = new HashMap<String, Player>();
	private Map<String, Integer> playerBoards = new LinkedHashMap<String, Integer>();
	private Map<String, String> playerCase = new HashMap<String, String>();
	private boolean needToSave;
	private static final Charset encoding = Charset.forName("UTF-8");

	public TournamentService(String fileName) {
		this.fileName = fileName;
	}

	public void load() throws IOException {
		FileInputStream fileIn = null;
		InputStreamReader readIn = null;
		BufferedReader in = null;
		try {
			fileIn = new FileInputStream(fileName);
			readIn = new InputStreamReader(fileIn, encoding);
			in = new BufferedReader(readIn);
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0)
					continue;
				String[] args = line.split("[ \t]+");
				String player = args[1];
				String game = args[2];
				int gameNum = Integer.parseInt(game);
				playerBoards.put(player, gameNum);
				playerCase.put(player.toLowerCase(), player);
			}
		} finally {
			FileHelper.close(in);
			FileHelper.close(readIn);
			FileHelper.close(fileIn);
		}
	}

	public void save() throws IOException {
		if (!needToSave)
			return;
		FileOutputStream fileOut = null;
		OutputStreamWriter writeOut = null;
		PrintWriter out = null;
		try {
			fileOut = new FileOutputStream(fileName);
			writeOut = new OutputStreamWriter(fileOut, encoding);
			out = new PrintWriter(writeOut);
			for (Map.Entry<String, Integer> entry : playerBoards.entrySet()) {
				String player = entry.getKey();
				int board = entry.getValue();
				String line = MessageFormat.format("reserve-game {0} {1}", player, board);
				out.println(line);
			}
		} finally {
			FileHelper.close(out);
			FileHelper.close(writeOut);
			FileHelper.close(fileOut);
		}
	}

	public void clearSchedule() {
		needToSave = true;
		playerBoards.clear();
		playerCase.clear();
	}

	public Player findPlayer(String name) {
		Player p = players.get(name);
		if (p == null) {
			p = new Player();
			p.setUserName(name);
			addPlayer(p);
		}
		return p;
	}

	public void addPlayer(Player p) {
		needToSave = true;
		String handle = p.getHandle();
		players.put(handle, p);

	}

	public void schedule(String white, String black, int board) {
		reserveBoard(white, board);
		reserveBoard(black, board);
	}

	public void reserveBoard(String player, int board) {
		unreserveBoard(player);
		needToSave = true;
		playerBoards.put(player, board);
		playerCase.put(player.toLowerCase(), player);
	}

	public int unreserveBoard(String player) {
		needToSave = true;
		player = player.toLowerCase();
		player = playerCase.remove(player);
		if (player == null)
			return -1;
		Integer board = playerBoards.remove(player);
		if (board == null)
			return -1;
		return board;
	}

	public int getPlayerBoard(String player) {
		player = player.toLowerCase();
		player = playerCase.get(player);
		if (player == null)
			return -1;
		Integer board = playerBoards.get(player);
		if (board == null)
			return -1;
		return board;
	}

	public Map<String, Integer> getPlayerBoardMap() {
		return Collections.unmodifiableMap(playerBoards);
	}

}
