package de.kuschku.ircbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

import com.google.gson.JsonObject;

import de.kuschku.ircbot.handlers.QuakeNetLoginHandler;

public class Client {

	JsonObject fileConfiguration;
	PircBotX bot;
	static Client client;
	public Options options;

	public static void main(String[] args) {
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
			new Client(options);
		} catch (CmdLineException e) {
			e.printStackTrace();
		}
	}

	public Client(Options options) {
		Client.client = this;
		this.options = options;
		try {
			fileConfiguration = FileConfiguration.fromFile(new File(
					options.configpath));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		JsonObject connection = fileConfiguration.getAsJsonObject("connection");

		// Setup this bot
		Configuration.Builder<PircBotX> builder = new Configuration.Builder<PircBotX>()
				.setName(connection.getAsJsonObject("name").get("nick").getAsString())
				.setLogin(connection.getAsJsonObject("name").get("login").getAsString())
				.setRealName(connection.getAsJsonObject("name").get("real").getAsString())
				.setAutoNickChange(false).setCapEnabled(true)
				.setServerHostname(connection.get("host").getAsString());
		
		PluginHandler handler = new PluginHandler(builder);
		handler.addHandler("de.kuschku.ircbot.handlers.ControlHandler");

		switch (connection.getAsJsonObject("authentication").get("type").getAsString()) {
		case "NickServ":
			builder.setNickservPassword(connection.getAsJsonObject("authentication").get("password").getAsString());
			break;
		case "TheQBot":
			handler.addHandler(QuakeNetLoginHandler.class.getCanonicalName());
			break;
		default:
			break;
		}

		connection.get("channels").getAsJsonArray().forEach(element -> builder.addAutoJoinChannel(element.getAsString()));
		
		handler.loadPlugins(new File(options.pluginpath));
		handler.build();

		this.bot = new PircBotX(builder.buildConfiguration());

		try {
			this.bot.startBot();
		} catch (Exception ex) {
		}
	}

	public static class Options {
		@Option(name = "-config") String configpath = "config.json";
		@Option(name = "-plugins")
		public String pluginpath = "plugins";
	}

	public static void log(Level level, String message) {
		Logger.getLogger(Client.class.getCanonicalName()).log(level, message);
	}
	
	public static JsonObject getConfig(String className) {
		List<String> privilegedList = new ArrayList<String> ();
		privilegedList.add(QuakeNetLoginHandler.class.getCanonicalName());
		
		if (privilegedList.contains(className)) {
			return Client.getClient().fileConfiguration;
		} else if (Client.getClient().fileConfiguration.getAsJsonObject("plugins").has(className)) {
			return Client.getClient().fileConfiguration.getAsJsonObject("plugins").getAsJsonObject(className);
		} else {
			return new JsonObject();
		}
	}
	
	public static final Client getClient() {
		return Client.client;
	}
	
	public void reloadConfig() {
		try {
			fileConfiguration = FileConfiguration.fromFile(new File(
					options.configpath));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
}
