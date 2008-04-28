package ch.ethz.iks.r_osgi.sample.concierge.shell;

import ch.ethz.iks.concierge.shell.commands.ShellCommandGroup;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.URI;

public class ShellPlugin implements ShellCommandGroup, RemoteServiceListener {

	private RemoteOSGiService remote;

	ShellPlugin(RemoteOSGiService remote) {
		this.remote = remote;
	}

	public String getGroup() {
		return "remote";
	}

	public String getHelp() {
		return "remote";
	}

	public void handleCommand(String command, String[] args) throws Exception {
		String c = command.intern();
		if (c == "connect") {
			if (args.length != 1) {
				System.err.println("usage: remote.connect <uri>");
				return;
			}
			System.out.println("connecting to... " + args[0]);
			remote.connect(new URI(args[0]));
			System.out.println("connected.");
		}
	}

	public void remoteServiceEvent(RemoteServiceEvent event) {
		if (event.getType() == RemoteServiceEvent.REGISTERED) {
			System.out.println("NEW SERVICE REGISTERED " + event.getRemoteReference().getURI());
		} else if (event.getType() == RemoteServiceEvent.UNREGISTERING) {
			System.out.println("SERVICE UNREGISTERED " + event.getRemoteReference().getURI());
		} else if (event.getType() == RemoteServiceEvent.MODIFIED) {
			System.out.println("SERVICE MODIFIED " + event.getRemoteReference().getURI());
		}
		
	}

}
