import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.imageio.ImageIO;

import org.powerbot.game.api.methods.Environment;
import org.powerbot.game.api.methods.Game;
import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.input.Keyboard;
import org.powerbot.game.api.methods.input.Mouse;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.widget.Camera;

public class RunescapeMobileServer {
	ServerSocket server;
	Socket socket;
	PrintWriter out;
	public BufferedImage img;
	private boolean connected, isScreenAvailable;
	private float clientX, clientY;
	private float clientWidth, clientHeight, serverWidth, serverHeight;
	private float scale, scaleWidth, scaleHeight;
	private long packetSize;
	
	// Constructor to allocate a ServerSocket listening at the given port.
	public RunescapeMobileServer(int port, float scale) {
		try {
			connected = false;
			isScreenAvailable = false;
			server = new ServerSocket(port);
			System.out.println("ServerSocket: " + server);
			clientX = 0;
			clientY = 0;
			this.scale = scale;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  Sends a message to the client
	 */
	 public void sendMessage(String message) throws IOException {
		 out = new PrintWriter(socket.getOutputStream());
		 out.println("Message:" + message);
		 out.flush();  // need to flush a short message
	}
	 
	 /**
	  * Sends a command to the client.
	  */
	 public void sendCommand(String command, int bit) throws IOException {
		 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		 out.writeInt(command.getBytes("UTF-8").length+1);
		 //This writes a single byte
		 out.write((byte) bit);
		 out.write(command.getBytes("UTF-8"));
		 out.flush();
	 }
	   
	   /**
	    * Sends a screenshot to the client
	    * @param message
	    * @throws IOException 
	    */
	   public void sendScreenshot(byte[] buffer) throws IOException {
		   DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		   out.writeInt(buffer.length);
		   // This writes a single byte
		   //out.write((byte) 0);
		   out.write(buffer);
		   out.flush();
	   }
	   
	   /**
	    * Add a command prefix to a byte array.
	    */
	   public byte[] addCommandToBytes(byte command, byte[] buffer, int Length ) {
		   byte[] b = new byte[Length+1];
		   b[0] = command;
		   System.arraycopy(buffer, 0, b, 1, Length);
		   return b;
	   }
	   
	   
	   
	   /**
	    * Grabs a screenshot.
	    */
	   public byte[] grabScreenshot() {
		   BufferedImage bim = Environment.captureScreen();
		   img = resize(bim, clientWidth, clientHeight);
		   byte[] result = null;
		   try {
			   ByteArrayOutputStream out = new ByteArrayOutputStream();
			   ImageIO.write(img, "GIF", out);
			   result = out.toByteArray();
			   return result;
			 } catch (Exception e) {
				   e.printStackTrace();
				   return result;
			}
	   }
	   
	   public static BufferedImage resize(BufferedImage img, float newW, float newH) {
			int w = img.getWidth();
			int h = img.getHeight();
			BufferedImage dimg = new BufferedImage((int) newW, (int) newH, img.getType());
			Graphics2D g = dimg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(img, 0, 0, (int) newW, (int) newH, 0, 0, w, h, null);
			g.dispose();
			return dimg;
		}

	   /**
	    * Forces the server to quit the block on .Accept.
	    * @throws IOException 
	    */
	   public void forceCloseAccept(){
		   try {
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   
	   
	   /**
	    * Closes the connection.
	    * @throws IOException 
	    */
	   public void closeConnection(){
		   
		   try {
			socket.setSoLinger(true, 0);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		   
		   try {
			sendCommand("DISCONNECT", 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	   }
	   
	   // Start listening.
	   public void listen() {
		   System.out.println("[RSB] - Listening for incoming connections...");
	      //while (true) { // run until you terminate the program
	         try {
	            // Wait for connection. Block until a connection is made.
	            socket = server.accept();
	            System.out.println("[RSB] Connected: " + socket);
	            // Start a new thread for each client to perform block-IO operations.
	            new ClientThread(socket).start();
	         } catch (IOException e) {
	            e.printStackTrace();
	         }
	     // }
	   }
	   
	   /**
	    * Keeps the socket alive..
	    * @param alive
	    */
	   public void setKeepAlive(boolean alive) {
		   try {
			socket.setKeepAlive(true);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   
	   public boolean isConnected() {
		   return connected;
	   }
	   
	   public boolean isScreenAvailable() {
		   return isScreenAvailable;
	   }
	   
	   public void setScreenAvailable(boolean available) {
		   isScreenAvailable = available;
	   }
	   
	   // Fork out a thread for each connected client to perform block-IO 
	   class ClientThread extends Thread {
	   
	      Socket socket;
	   
	      public ClientThread(Socket socket) {
	         this.socket = socket;
	      }
	      
	      /**
	       * Converts the byte array to a string.
	       * UTF-8 is used.
	     * @throws UnsupportedEncodingException 
	       */
	      private String bytesToString(byte[] buffer) throws UnsupportedEncodingException {
	    	  String str = new String(buffer, "UTF-8");
	    	  return str;
	    		
	      }
	   
	      @Override
	      public void run() {
	         InputStream in;
	         try {
	            in = socket.getInputStream();
	            byte[] buf = new byte[4096];
	            int byteRead;
	            
	            // Block until the client closes the connection, results in read() returns -1
	            while ((byteRead = in.read(buf)) != -1) {
	            	
	            	//A prefix is send with header on start.
	            	byte[] b = new byte[buf.length-4];
	     		   	System.arraycopy(buf, 4, b, 0, b.length);
	     		   	
	            	String command = bytesToString(b);
	            	
	            	//Command or Message:
	            	if (command.startsWith("Command:") == true) {
	            		command = command.replace("Command:", "");

	            		
	            		//Sends the screenshot to the client.		
	            		if (command.startsWith("REQUESTSCREEN") == true) {
	            			setScreenAvailable(true);
	            		
	            			//Mouse Left button was pressed
	            		} else if (command.startsWith("MOUSELB:") == true) {
	            			//MOUSELB:3.12:4.20:
	            			command = command.replace("MOUSELB:", "");
	            			String[] cmd = command.split(":");
	            			clientX = Float.valueOf(cmd[0].trim()).floatValue();
	            			clientY = Float.valueOf(cmd[1].trim()).floatValue();
	            			clientX = clientX * scaleWidth;
	            			clientY = clientY * scaleHeight;
	            			System.out.println("Clicked Mouse LB = X: " + clientX + " Y: " + clientY);
	            			Mouse.click((int) clientX, (int) clientY, true);
	            			
	  
	            			setScreenAvailable(true);
	            			
	            			//Mouse Right button was pressed
	            		} else if (command.startsWith("MOUSERB:") == true) {
	            			//MOUSERB:3.12:4.20:
	            			command = command.replace("MOUSERB:", "");
	            			String[] cmd = command.split(":");
	            			clientX = Float.valueOf(cmd[0].trim()).floatValue();
	            			clientY = Float.valueOf(cmd[1].trim()).floatValue();
	            			clientX = clientX * scaleWidth;
	            			clientY = clientY * scaleHeight;
	            			System.out.println("Clicked Mouse RB = X: " + clientX + " Y: " + clientY);
	            			Mouse.click((int) clientX, (int) clientY, false);
	            	
	            			setScreenAvailable(true);
	            			
	            		//Initialize the device configuration.
	            		} else if (command.startsWith("INITIALIZE:") == true) {
	            			command = command.replace("INITIALIZE:", "");
	            			
	            			//Calculates the scale for mouse click positioning.
	            			String[] cmd = command.split(":");
	            			clientWidth = Float.valueOf(cmd[0].trim()).floatValue();
	            			clientHeight = Float.valueOf(cmd[1].trim()).floatValue();
	            			clientWidth = clientWidth/scale;
	            			clientHeight = clientHeight/scale;
	            			
	            			System.out.println("Client Width:" + clientWidth);
	            			System.out.println("Client Height:" + clientHeight);
	            			
	            			BufferedImage bim = Environment.captureScreen();
	            			serverWidth = bim.getWidth();
	            			serverHeight = bim.getHeight();
	            			
	            			serverWidth = serverWidth/scale;
	            			serverHeight = serverHeight/scale;
	            			
	            			System.out.println("Server Width:" + serverWidth);
	            			System.out.println("Server Height:" + serverHeight);
	            			
	            			scaleWidth = serverWidth/clientWidth;
	            			scaleHeight = serverHeight/clientHeight;
	            			
	            			System.out.println("Scale Width:" + scaleWidth);
	            			System.out.println("Scale Height:" + scaleHeight);
	            			
	            			//Move mouse to position 0,0
	            			Mouse.hop(0, 0);
	            			
	            			connected = true;
	            			setScreenAvailable(true);
	            			
	            			//Move camera
	            		} else if (command.startsWith("DPADLEFT") == true) {
	            			System.out.println("Turning camera left.");
	            			int yaw = Camera.getYaw();
	            			Camera.setAngle(yaw-20);
	            			
	            		} else if (command.startsWith("DPADUP") == true) {
	            			System.out.println("Turning camera up.");
	            			int pitch = Camera.getPitch();
	            			Camera.setPitch(pitch+10);
	            			
	            		} else if (command.startsWith("DPADRIGHT") == true) {
	            			System.out.println("Turning camera right.");
	            			int yaw = Camera.getYaw();
	            			Camera.setAngle(yaw+20);
	            			
	            		} else if (command.startsWith("DPADDOWN") == true) {
	            			System.out.println("Turning camera down.");
	            			int pitch = Camera.getPitch();
	            			Camera.setPitch(pitch-10);
	            			
	            		} else if (command.startsWith("SWIPELEFT") == true) {
	            			System.out.println("Turning camera left.");
	            			int yaw = Camera.getYaw();
	            			Camera.setAngle(yaw-30);
	            			
	            		} else if (command.startsWith("SWIPEUP") == true) {
	            			System.out.println("Turning camera up.");
	            			int pitch = Camera.getPitch();
	            			Camera.setPitch(pitch+20);
	            			
	            		} else if (command.startsWith("SWIPERIGHT") == true) {
	            			System.out.println("Turning camera right.");
	            			int yaw = Camera.getYaw();
	            			Camera.setAngle(yaw+30);
	            			
	            		} else if (command.startsWith("SWIPEDOWN") == true) {
	            			System.out.println("Turning camera down.");
	            			int pitch = Camera.getPitch();
	            			Camera.setPitch(pitch-20);
	            			
	            		} else if (command.startsWith("INVENTORY") == true) {

	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Opening inventory");
	            				Tabs.INVENTORY.open();
	            			}
	            		} else if (command.startsWith("EQUIPMENT") == true) {

	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Opening equipment");
	            				Tabs.EQUIPMENT.open();
	            			}
	            		} else if (command.startsWith("STATS") == true) {

	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Opening inventory");
	            				Tabs.STATS.open();
	            			}
	            		} else if (command.startsWith("ABILITIES") == true) {

	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Opening Magic");
	            				Tabs.ABILITY_BOOK.open();
	            			}
	            		} else if (command.startsWith("PRAYER") == true) {

	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Opening prayer");
	            				Tabs.PRAYER.open();
	            			}
	            			
	            		} else if (command.startsWith("LOGOUT") == true) {
	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Logging out");
		            			Game.logout(true);
	            			}
	            		} else if (command.startsWith("LOGIN") == true) {
	            			if (Game.getClientState() == Game.INDEX_LOBBY_SCREEN) {
		            			System.out.println("Logging in from the lobby");
		            			//375,508
		            			Mouse.click(375, 508, true);
	            			}
	            			
	            		}
	            		
	            		//Disconnects
	            		if (command.startsWith("DISCONNECT") == true) {
	            			if (Game.isLoggedIn() == true) {
		            			System.out.println("Logging out");
		            			do {
		            				System.out.print("Trying to log out.");
		            				Game.logout(true);
		            			} while(Players.getLocal().isInCombat() == true);
		            			System.out.print("Logged out!");
	            			}
	            			connected = false;
	            			setScreenAvailable(false);
	            			System.out.println("[RSB] - Disconnecting...");
	            			sendCommand("DISCONNECTED", 2);
	            		} else {
	            			setScreenAvailable(true);
	            		}
	            		
	            	} else if (command.startsWith("Message:") == true) {
	            		String message = command.replace("Message:", "");
            			String[] msg = message.split(":");
            			int count = Integer.parseInt(msg[0]);

            			message = message.substring(msg[0].length()+1, count+msg[0].length()+1);
            			System.out.println("Message: " + message);
	            		Keyboard.sendText(message, true);
	            		setScreenAvailable(true);
	            		
	            	}
	            	
	            	//setScreenAvailable(true);
	            	buf = new byte[4096];
	            }
	            
	            server.close();
	            server = new ServerSocket();
	            System.out.println("[RSB] - Disconnected!");
	
	         } catch (IOException e) {
	            e.printStackTrace();
	         }
	      }
	   }
}