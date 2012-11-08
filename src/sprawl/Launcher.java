package sprawl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

public class Launcher extends JFrame implements ActionListener {
	protected JLabel status;
	protected static final String cdnUrl = "http://sprawl-releases.s3.amazonaws.com/";
	protected List<String> messages = new ArrayList<String>();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JScrollPane scrollStatus;

	public static void main(String[] args) {
		new Launcher();
	}

	public Launcher() {
		// Setup the window
		super("Sprawl Launcher");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(800, 600));
		this.setLocationByPlatform(true);
		
		// Create the news pane
		JPanel newsPanel = new JPanel(new BorderLayout());		
		((BorderLayout) newsPanel.getLayout()).setVgap(0);
		newsPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		String url = "http://blog.playsprawl.com/";
	    JEditorPane editorPane;
	    try {
			editorPane = new JEditorPane(url);
			editorPane.setEditable(false);
			editorPane.setBorder(null);
			
			JScrollPane scrollPane = new JScrollPane(editorPane);
			scrollPane.setBorder(null);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			newsPanel.add(BorderLayout.CENTER, scrollPane);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create the update pane
		JPanel updatePanel = new JPanel(new BorderLayout());
		updatePanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 100));
		updatePanel.setBackground(new Color(0, 0, 0));
		
		// Logo
		JLabel logo = new JLabel("", createImageIcon("/logo.png", "Sprawl Logo"), JLabel.LEFT);
		logo.setAlignmentY(CENTER_ALIGNMENT);
		updatePanel.add(BorderLayout.WEST, logo);

		// Play Button
		JButton playButton = new JButton("Play");
		playButton.setPreferredSize(new Dimension(200, 100));
		playButton.addActionListener(this);
		updatePanel.add(BorderLayout.EAST, playButton);
		
		// Status
		status = new JLabel("", SwingConstants.CENTER);
		status.setForeground(Color.black);
		scrollStatus = new JScrollPane(status);
		scrollStatus.setBorder(null);
		updatePanel.add(BorderLayout.CENTER, scrollStatus);
		
		// Add to frame
		this.getContentPane().add(BorderLayout.CENTER, newsPanel);
		this.getContentPane().add(BorderLayout.SOUTH, updatePanel);
		
		this.pack();
		this.setVisible(true);
	}

	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void actionPerformed(ActionEvent e) {
		messages.clear();
		JButton button = (JButton) e.getSource();
		button.setEnabled(false);
		
		String latestVersion = checkForUpdates();
		boolean isLatest = checkLocalVersion(latestVersion);
		
		if (isLatest) {
			addMessage("Up to date.");
			playGame();
		} else {
			// See if we need the binaries
			String osName = System.getProperty("os.name");
			String nativesPath = "lib/native/";
			String osDir = "";
			ArrayList<String> nativeFiles = new ArrayList<String>(); 
			if (osName.contains("Mac OS X")) {
				System.out.println("Installing Mac OS X Version");
				osDir = "macosx";
 				
				nativeFiles.add("libjinput-osx.jnilib");
				nativeFiles.add("liblwjgl.jnilib");
				nativeFiles.add("openal.dylib");
			} else if (osName.contains("Windows")) {
				System.out.println("Installing Winows Version");
				osDir = "windows";
 				
				nativeFiles.add("OpenAL64.dll");
				nativeFiles.add("OpenAL32.dll");
				nativeFiles.add("lwjgl64.dll");
				nativeFiles.add("lwjgl.dll");
				nativeFiles.add("jinput-raw_64.dll");
				nativeFiles.add("jinput-raw.dll");
				nativeFiles.add("jinput-dx8_64.dll");
				nativeFiles.add("jinput-dx8.dll");
			}
			File nativesDir = new File(nativesPath + osDir);
			addMessage("Checking natives installation...");
			if (!nativesDir.exists()) {
				nativesDir.mkdirs();
				addMessage("Installing Natives:");
				
				//Get Files from S3
				for (String filename : nativeFiles) {
					addMessage(" - " + filename);
					try {
						URL website = new URL(cdnUrl + "natives/" + osDir + "/" + filename);
					    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
					    new FileOutputStream(nativesPath + osDir + "/" + filename).getChannel().transferFrom(rbc, 0, 1 << 24);
					} catch(Exception e1) {
						addMessage("There was an error downloading: " + filename);
						e1.printStackTrace();
						return;
					}
				}
			} else {
				addMessage(" - found!");
			}
			
			// Download the latest JAR
			addMessage("Downloading updates...");
			try {
				URL website = new URL(cdnUrl + "releases/" + latestVersion + "/Sprawl.jar");
			    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			    new FileOutputStream("./Sprawl.jar").getChannel().transferFrom(rbc, 0, 1 << 24);
			    
			    // Update the Version File
			    updateLocalVersion(latestVersion);
			    addMessage("Update to " + latestVersion + " Complete!");
			    
			    playGame();
			} catch(Exception e1) {
				addMessage("There was an error downloading Updates.");
				e1.printStackTrace();
				return;
			}
		}
		
		button.setEnabled(true);
	}
	
	public void playGame() {
		addMessage("Launching game...");
		File file = new File("Sprawl.jar");
		
		try {
			Process proc = Runtime.getRuntime().exec("java -jar " + file.getAbsolutePath());
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		URLClassLoader cl;
//		try {
//			URL jarURL = file.toURI().toURL();
//			cl =  URLClassLoader.newInstance(new URL[] {jarURL});
//			Class gameClass = cl.loadClass("sprawl.Game");
//			Method gameLoop = gameClass.getMethod("gameLoop");
//			Object MyClassObj = gameClass.newInstance();
//			Object response = gameLoop.invoke(MyClassObj);
//			
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}        
	}
	
	public void addMessage(String message) {
		messages.add(message);
		status.setText("<html>" + this.join(messages, "<br />") + "</html>");
		scrollStatus.getVerticalScrollBar().setValue(scrollStatus.getVerticalScrollBar().getMaximum());
	}
	
	public boolean checkLocalVersion(String latestVersion) {
		// Get the local version
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("./VERSION"));
	        String currentVersion = br.readLine();
	        br.close();
	        
	        addMessage("Local Version " + currentVersion);
	        return latestVersion.equals(currentVersion);
	    } catch (IOException e) {
	    	addMessage("Local Version Not Found");
	    }
		
		return false;
	}
	
	public void updateLocalVersion(String latestVersion) throws IOException {
		Writer out = new OutputStreamWriter(new FileOutputStream("./VERSION"));
		try {
			out.write(latestVersion);
	        addMessage("Local Version " + latestVersion);
	    } catch (IOException e) {
	    	addMessage("Couldn't update VERSION file.");
	    } finally {
	    	out.close();
	    }
	}
	
	public String checkForUpdates() {
		addMessage("Checking for Updates...");
		String latestVersion;
		// Get the latest version
		BufferedReader in;
		try {
			URL url = new URL(cdnUrl + "VERSION");
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			latestVersion = in.readLine();
			in.close();
		    addMessage("Latest Version " + latestVersion);
		    return latestVersion;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			status.setText("There was an error connecting to the Sprawl servers");
		}
		return null;
	}
	
	public String join(Collection<String> s, String delimiter) {
	    StringBuffer buffer = new StringBuffer();
	    Iterator<String> iter = s.iterator();
	    while (iter.hasNext()) {
	        buffer.append(iter.next());
	        if (iter.hasNext()) {
	            buffer.append(delimiter);
	        }
	    }
	    return buffer.toString();
	}
}