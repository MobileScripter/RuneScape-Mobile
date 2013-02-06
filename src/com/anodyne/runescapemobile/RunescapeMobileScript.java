import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.powerbot.core.script.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.util.Random;
import org.powerbot.game.bot.Context;


@Manifest(authors = { "MobileScripter" }, name = "RuneScape Mobile", description = "Play RuneScape on your Android powered device!")
public class RunescapeMobileScript extends ActiveScript {

	private RunescapeMobileServer ssl;
	private boolean firstTime = true;
	private InetAddress iNetAddress;
	private String ipAddress, port, scale;
	private JLabel lblState;
	private JProgressBar progressBar;
	private JButton btnState;
	private JFrame frame;
	private long packetSize;
	private long amountPacket;
	
	public void onStart() {
		
		//Initialization
		try {
			iNetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ipAddress = iNetAddress.getHostAddress();
		
		//Loads the User Interface.
		JOptionPane.showMessageDialog(null, "Welcome to RuneScape Mobile.\nThis beta version will stream live screenshots from your desktop to your Android-Powered device.\nMake sure your device and computer are both connected to the same network.", "RuneScape Mobile", JOptionPane.INFORMATION_MESSAGE );
		ipAddress = JOptionPane.showInputDialog("Enter IP Address.", iNetAddress.getHostAddress());
		port = JOptionPane.showInputDialog("Enter port number.", "10130");
		do {
			scale = JOptionPane.showInputDialog("Enter scale.\n This can be any number between 1 and 5.\n The higher the number, the higher the FPS (Frames Per Second) but suffer image quality loss.", "2.5");
		} while ((Double.parseDouble(scale)>0 && Double.parseDouble(scale)<6) == false);
		
		//Where the GUI is constructed:
		progressBar = new JProgressBar(0);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(true);
		progressBar.setBounds(40, 20, 220, 25);
		
		//Constructs a new frame
		frame = new JFrame("RuneScape Mobile Beta v1.0");
				
		//Sets the width and height
		frame.setSize(300, 200);
		frame.setLayout(null);
				
		 // Get the size of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			    
		// Determine the new location of the window
		int w = frame.getSize().width;
		int h = frame.getSize().height;
		int x = (dim.width-w)/2;
		int y = (dim.height-h)/2;
			    
		// Move the window
		frame.setLocation(x, y);
		
		//Add objects to frame
		frame.add(progressBar);
		lblState = addLabel(frame, 0, 50, 300, 50, "<html>Waiting for connection...<br>IP Address: " + ipAddress + "</html>", 12, Color.BLACK);
		lblState.setHorizontalAlignment(SwingConstants.CENTER);
		lblState.setVerticalAlignment(SwingConstants.CENTER);
		frame.add(lblState);
		
		//Add button
	    btnState = new JButton();
	    btnState.setText("Cancel");
	    btnState.setEnabled(false);
	    
	    btnState.setBounds(200, 140, 75, 25);
	    btnState.addActionListener(mouseClick);
	    
	    frame.add(btnState);

	    
	    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

	    frame.addWindowListener(windowAdapter);
	    
	    frame.setResizable(false);
	
		frame.setVisible(true);
		
	
		//Starts Listening.
		ssl = new RunescapeMobileServer(getPort(), getScale());
		ssl.listen();
		firstTime = false;
				
		
	}
	
	WindowAdapter windowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent ev) {
			int i = JOptionPane.showOptionDialog(null, "Are you sure you want to quit?","RuneScape Mobile", JOptionPane.OK_OPTION, 
					JOptionPane.PLAIN_MESSAGE, null, null, null);
			if (i == JOptionPane.OK_OPTION) {
				if (ssl.isConnected()==false) {
					ssl.forceCloseAccept();
				} else {
					ssl.closeConnection();
				}
				frame.dispose(); //Destroy the JFrame object
	            Context.get().getScriptHandler().stop();
			}
            
        }
	};
	
	public void onStop() {
		System.out.println(amountPacket + " packets has been sent with a size of " + packetSize + " bytes.");
		System.out.println("[RSB] - Stopped!");
	}
		
	
	@Override
	public int loop() {
		
		if (firstTime == false) {
			if (ssl.isConnected() == true && ssl.isScreenAvailable() == true) {
				lblState.setText("Connected");
				btnState.setEnabled(true);
				byte[] btmp = ssl.grabScreenshot();
				System.out.println("Length of buffer is: " + btmp.length);
				packetSize += btmp.length;
				amountPacket++;
				try {
					ssl.sendScreenshot(btmp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("ERROR! Message: " + e.getMessage());
				}
				
			} 
			
			if (ssl.isConnected() == false) {
				btnState.setEnabled(false);
				progressBar.setValue(progressBar.getMinimum());
				progressBar.setIndeterminate(false);
				lblState.setText("Disconnected");
			}
			
			ssl.setScreenAvailable(false);
		}
		

		return Random.nextInt(30, 60);
	}
	
	
	ActionListener mouseClick = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			//Note: work with if's, no switch
			System.out.println(amountPacket + " packets has been sent with a size of " + packetSize + " bytes.");
			ssl.closeConnection();
			
		}
		
	};
	
	/**
	 * Returns the chosen IP Address.
	 * @return
	 */
	public String getIPAddress() {
		return ipAddress;
	}
	
	/**
	 * Returns the chosen port.
	 * @return
	 */
	public int getPort() {
		return Integer.parseInt(port);
	}
	
	/**
	 * Returns the desired scale.
	 * @return
	 */
	public float getScale() {
		return Float.valueOf(scale.trim()).floatValue();
	}
	
	/**
	 * Return the state label.
	 * @return
	 */
	public JLabel getStateLabel() {
		return lblState;
	}
	
	/**
	 * Adds a new label to the frame.
	 */
	private JLabel addLabel(JFrame frame, int x, int y, int width, int height, String text, int size, Color color) {
		JLabel label = new JLabel();
		label.setText(text);
		label.setBounds(x, y, width, height);
		label.setForeground(color);
		return label;
	}
}



