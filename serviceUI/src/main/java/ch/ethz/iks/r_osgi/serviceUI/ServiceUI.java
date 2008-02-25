/* Copyright (c) 2006 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package ch.ethz.iks.r_osgi.serviceUI;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;
import ch.ethz.iks.r_osgi.types.ServiceUIComponent;

/**
 * @author Jan S. Rellermeyer, ETH Zurich
 */
class ServiceUI extends Frame implements RemoteServiceListener,
		ServiceDiscoveryListener {

	private static final long serialVersionUID = 7556679981800636909L;

	private CardLayout cards;

	private Panel selectorPanel;

	private Panel displayPanel;

	private Panel statusPanel;

	private Choice selector;

	private Component background;

	private ArrayList panels = new ArrayList(2);

	private int currentPanel;

	private Choice service;

	private Label statusLine;

	private Button connect;

	private HashMap knownServices = new HashMap(2);

	private HashMap serviceString_panel = new HashMap(2);

	private Set discoveredServices = new HashSet();

	ServiceUI() {
		super("R-OSGi ServiceUI");
		Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
		int width = size.width < 300 ? size.width : 300;
		int heigth = size.height < 400 ? size.height - 10 : 400;
		setSize(width, heigth);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setLayout(new BorderLayout());
		selectorPanel = new Panel() {
			private static final long serialVersionUID = -7133785104429233021L;

			public void paint(Graphics g) {
				super.paint(g);
				try {
					g.drawLine(0, getY(), getX() + getWidth(), getY());
				} catch (Throwable t) {
					// FIXME: PJava does not provide the get... methods.
				}
			}
		};
		selector = new Choice();
		selector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				String panelName = (String) evt.getItem();
				currentPanel = panels.indexOf(panelName);
				cards.show(displayPanel, panelName);
				displayPanel.invalidate();
				validate();
			}
		});
		selectorPanel.add(selector);

		cards = new CardLayout();
		displayPanel = new Panel();
		displayPanel.setLayout(cards);

		background = new Panel() {
			private static final long serialVersionUID = -7719998254380318187L;

			final Image background = Toolkit.getDefaultToolkit().getImage(
					"R-OSGi.jpg");

			public void paint(Graphics g) {
				super.paint(g);
				Dimension size = this.getSize();
				g.setColor(Color.lightGray);
				g.fillRect(getX(), getY(), getWidth(), getHeight());

				g.drawImage(background,
						(size.width - background.getWidth(null)) / 2,
						(size.height - background.getHeight(null)) / 2, this);
			}

		};
		displayPanel.add(background, "background");
		panels.add("background");

		statusPanel = new Panel() {
			private static final long serialVersionUID = 1L;

			public void paint(Graphics g) {
				super.paint(g);
				g.drawLine(1, 1, getX() + getWidth(), 1);
			}
		};
		statusPanel.setBackground(Color.gray);
		statusLine = new Label("Idle ...");
		connect = new Button("connect");
		connect.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				new Dialog(new Frame(), "R-OSGi ServiceUI") {

					static final long serialVersionUID = 1L;

					public void setVisible(final boolean visible) {
						if (visible) {
							setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
							add(new Label("Connect to:"), BorderLayout.WEST);
							final TextField hostaddress = new TextField(10);
							add(hostaddress, BorderLayout.CENTER);
							final Button c = new Button("Connect");
							c.addActionListener(new ActionListener() {
								public void actionPerformed(final ActionEvent e) {
									try {
										URI endpoint = new URI(hostaddress
												.getText());
										final RemoteServiceReference[] refs = ServiceUIActivator.remote
												.connect(endpoint);
										new Dialog(new Frame(),
												"R-OSGi ServiceUI") {
											private static final long serialVersionUID = -5207305068261925447L;

											public void setVisible(
													boolean visible) {
												if (visible) {
													setLayout(new BorderLayout());
													final List list = new List();
													for (int i = 0; i < refs.length; i++) {
														list.add(refs[i]
																.getURI()
																.toString());

													}
													list
															.addActionListener(new ActionListener() {
																public void actionPerformed(
																		final ActionEvent e) {
																	fetchService(list
																			.getSelectedItem());
																	setVisible(false);
																}
															});
													add(list,
															BorderLayout.CENTER);
													Button button = new Button(
															"close");
													button
															.addActionListener(new ActionListener() {

																public void actionPerformed(
																		ActionEvent e) {
																	setVisible(false);
																}

															});
													add(button,
															BorderLayout.SOUTH);
													pack();
												}
												super.setVisible(visible);
											}
										}.setVisible(true);
									} catch (RemoteOSGiException e1) {
										e1.printStackTrace();
									} catch (IOException ioe) {
										
									}
									setVisible(false);
								}
							});
							add(c, BorderLayout.EAST);
							pack();
						}
						super.setVisible(visible);
					}
				}.setVisible(true);
			}

		});

		service = new Choice();
		service.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				String serviceName = (String) evt.getItem();
				fetchService(serviceName);
				service.remove(serviceName);
			}
		});
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(connect, BorderLayout.WEST);
		statusPanel.add(statusLine, BorderLayout.CENTER);
		statusPanel.add(service, BorderLayout.EAST);
		add(statusPanel, BorderLayout.SOUTH);
		add(selectorPanel, BorderLayout.NORTH);
		add(displayPanel, BorderLayout.CENTER);

		setVisible(true);
	}

	private void fetchService(final String service) {
		final String uri = service.substring(service.indexOf("@") + 1);

		try {
			RemoteServiceReference ref = (RemoteServiceReference) knownServices
					.get(uri);

			if (ref == null) {
				ref = ServiceUIActivator.remote
						.getRemoteServiceReference(new URI(uri));
				knownServices.put(uri, ref);
			}

			ServiceUIActivator.remote.getRemoteService(ref);
			if (ref != null) {
				String presentation = (String) ref
						.getProperty(RemoteOSGiService.PRESENTATION);

				if (presentation != null) {

					StringBuffer buffer = new StringBuffer();
					buffer.append("(&(");
					buffer.append(RemoteOSGiService.SERVICE_URI);
					buffer.append('=');
					buffer.append(ref.getURI().toString());
					buffer.append(")(");
					buffer.append(RemoteOSGiService.PRESENTATION);
					buffer.append('=');
					buffer.append(presentation);
					buffer.append("))");
					ServiceReference[] presRefs = ServiceUIActivator.context
							.getServiceReferences(ServiceUIComponent.class
									.getName(), buffer.toString());

					if (presRefs != null) {
						ServiceUIComponent comp = (ServiceUIComponent) ServiceUIActivator.context
								.getService(presRefs[0]);
						addPanel(ref.getURI().toString(), comp.getPanel());
					} else {
						System.err.println("No registration matches "
								+ buffer.toString());
					}
				} else {
					statusLine.setText("No presentation found.");
					new CleanStatusLineThread();
				}
			}
		} catch (RemoteOSGiException e) {
			e.printStackTrace();
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}

	}

	private void addPanel(String name, Panel panel) {
		serviceString_panel.put(name, panel);
		selector.add(name);
		selector.invalidate();
		panels.add(name);
		displayPanel.add(name, panel);
		validate();
		selector.select(name);
		currentPanel = panels.indexOf(name);
		cards.show(displayPanel, name);
		displayPanel.invalidate();
	}

	private void removePanel(String name) {
		Panel panel = (Panel) serviceString_panel.remove(name);
		if (panel == null) {
			return;
		}
		selector.remove(name);
		selector.invalidate();
		if (panels.indexOf(name) == currentPanel) {
			displayPanel.remove(panel);
			panels.remove(name);
			currentPanel--;
			cards.show(displayPanel, (String) panels.get(currentPanel));
		} else {
			String current = (String) panels.get(currentPanel);
			displayPanel.remove(panel);
			panels.remove(name);
			currentPanel = panels.indexOf(current);
		}
		validate();
		repaint();
	}

	private class CleanStatusLineThread extends Thread {
		private CleanStatusLineThread() {
			start();
		}

		public void run() {
			synchronized (this) {
				try {
					this.wait(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				statusLine.setText("idle ...");
			}
		}
	}

	public void remoteServiceEvent(final RemoteServiceEvent event) {
		final RemoteServiceReference ref = event.getRemoteReference();
		final String uri = event.getRemoteReference().getURI().toString();
		final String[] interfaces = (String[]) ref
				.getProperty(Constants.OBJECTCLASS);

		switch (event.getType()) {
		case RemoteServiceEvent.REGISTERED:
			knownServices.put(uri, ref);
			if (!discoveredServices.contains(uri)) {
				statusLine.setText("New " + uri);
				new CleanStatusLineThread();
				service.add(Arrays.asList(interfaces) + "@" + uri);
				service.invalidate();
				validate();
			}
			return;
		case RemoteServiceEvent.UNREGISTERING:
			knownServices.remove(uri);
			discoveredServices.remove(uri);
			statusLine.setText("Lost " + uri);
			new CleanStatusLineThread();
			removePanel(Arrays.asList(interfaces) + "@" + uri);
			service.invalidate();
			validate();
			return;
		}

	}

	public void announceService(String serviceInterface, URI uri) {
		discoveredServices.add(uri.toString());
		statusLine.setText("New " + uri);
		new CleanStatusLineThread();
		service.add(serviceInterface + "@" + uri.toString());
		service.invalidate();
		validate();
		return;
	}

	public void discardService(String serviceInterface, URI uri) {
		discoveredServices.remove(uri.toString());
		statusLine.setText("Lost " + uri);
		new CleanStatusLineThread();
		final String serviceStr = serviceInterface + "@" + uri.toString();
		for (int i = 0; i < service.getItemCount(); i++) {
			if (service.getItem(i).equals(serviceStr)) {
				service.remove(serviceStr);
				service.invalidate();
				continue;
			}
		}
		removePanel(uri.toString());
		validate();
		return;
	}
}