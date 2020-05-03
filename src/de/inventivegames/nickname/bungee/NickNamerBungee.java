package de.inventivegames.nickname.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.inventivetalent.update.bungee.BungeeUpdater;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NickNamerBungee extends Plugin implements Listener {

	Map<InetSocketAddress, String[]> shutdownData = new HashMap<>();

	@Override
	public void onEnable() {
		ProxyServer.getInstance().registerChannel("NickNamer");
		ProxyServer.getInstance().getPluginManager().registerListener(this, this);

		try {
			BungeeUpdater updater = new BungeeUpdater(this, 8964);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent e) {
		if ("NickNamer".equals(e.getTag())) {
			ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
			ByteArrayDataOutput out = ByteStreams.newDataOutput();

			List<String> data = new ArrayList<>();

			String line = null;
			int length = 0;
			try {
				while ((line = in.readUTF()) != null && length < 5) {//None of the messages contains more than 5 elements
					data.add(line);
					out.writeUTF(line);
					length++;
				}
			} catch (Exception e1) {
			}

			if (data.size() < 3) {
				getLogger().warning("Received message with invalid content length (" + data.size() + " < 3, " + data.toString() + ")");
				return;
			}

			//Handle the shutdown message
			if ("shutdown".equals(data.get(0))) {
				String nameString = data.get(2);
				String skinString = data.get(3);
				String dataString = data.get(4);

				if (e.getSender() instanceof ServerConnection) {
					shutdownData.put(((ServerConnection) e.getSender()).getAddress(), new String[] {
							nameString,
							skinString,
							dataString });//Store the data
				}

				return;
			}

			Connection receiver = e.getReceiver();
			if (receiver instanceof ProxiedPlayer) {
				ProxiedPlayer player = ((ProxiedPlayer) receiver);
				for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
					if (!server.getPlayers().contains(player)) {
						server.sendData("NickNamer", out.toByteArray(), true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onServerConnect(ServerConnectEvent e) {
		InetSocketAddress address = e.getTarget().getAddress();
		if (shutdownData.containsKey(address)) {
			String[] data = shutdownData.get(address);
			if (data == null || data.length == 0) { return; }
			String dataString = data[2];

			if (!dataString.isEmpty()) {
				String[] split = dataString.split("\\+#\\+");
				for (String s : split) {
					String[] split1 = s.split("=#=");
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("data");
					out.writeUTF(split[0]);//This is the target UUID, but data doesn't need a valid one
					out.writeUTF(split1[0]);
					out.writeUTF(split1[1]);

					e.getTarget().sendData("NickNamer", out.toByteArray(), true);
				}
			}
		}
	}

	@EventHandler
	public void onServerConnected(ServerConnectedEvent e) {
		InetSocketAddress address = e.getServer().getInfo().getAddress();
		if (shutdownData.containsKey(address)) {
			String[] data = shutdownData.get(address);
			if (data == null || data.length == 0) { return; }
			String nameString = data[0];
			String skinString = data[1];
			shutdownData.remove(address);

			if (!nameString.isEmpty()) {
				String[] split = nameString.split("\\+#\\+");
				for (String s : split) {
					String[] split1 = s.split("=#=");
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("name");
					out.writeUTF(split1[0]);
					out.writeUTF(split1[1]);

					e.getServer().getInfo().sendData("NickNamer", out.toByteArray(), true);
				}
			}
			if (!skinString.isEmpty()) {
				String[] split = skinString.split("\\+#\\+");
				for (String s : split) {
					String[] split1 = s.split("=#=");
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("skin");
					out.writeUTF(split1[0]);
					out.writeUTF(split1[1]);

					e.getServer().getInfo().sendData("NickNamer", out.toByteArray(), true);
				}
			}
		}
	}

}
