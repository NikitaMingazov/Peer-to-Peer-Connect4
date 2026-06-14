/*
	A2 written by Nikita Mingazov
	last modified: 27/10/24
	This networks with peers, creates and runs a game
	It closes after a game finishes
*/
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.net.SocketTimeoutException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.io.IOException;

public class A2 {

	private static boolean haventPlayedYet = true;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java A2 <IP> <PORT>");
		}
		try {
			InetAddress dAdd = InetAddress.getByName(args[0]);
			int broadcastPort = Integer.parseInt(args[1]);

			DatagramSocket socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.bind(new java.net.InetSocketAddress(broadcastPort)); // idk what this is doing but chatgpt patched this

			socket.setSoTimeout(10000); // wait 10 seconds
			// boolean haventPlayedYet = true; (I moved this to be a private variable to extend it's scope into methods)
			while (haventPlayedYet) {
				boolean received = true; // used to discern whether the code timed out or not
				byte[] buffer = new byte[13];
				DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);

				try {
					socket.receive(inPacket);
				}
				catch (SocketTimeoutException e)
				{
					System.out.println("UDP listening timed out, now transmitting");
					received = false;
				}
				if (received) { // message found
					String inMsg = new String(buffer, StandardCharsets.UTF_8);
					System.out.println("Received " + inMsg);
					int clientPort;
					if (inMsg.substring(0,8).equals("NEW GAME")) {
						InetAddress clientIP = inPacket.getAddress();
						clientPort = Integer.parseInt(inMsg.substring(9,13));
						if (clientPort < 9000 || clientPort > 9100) {
							continue; // if the port given is outside of playing range, ignore and go back to listening
						}
						try (
							Socket s = new Socket(clientIP, clientPort);
							InputStream in = s.getInputStream();
							OutputStream out = s.getOutputStream();
							BufferedReader terminalInput = new BufferedReader(new InputStreamReader(System.in))
						) {
							Connect4 game = new Connect4();
							buffer = new byte[9];
							boolean yourTurn = false; // player 2
							while (true) {
								int result = -1;
								if (yourTurn) {
									result = makeMove(game, out, terminalInput, 2);
								}
								else { // other player's turn
									result = waitForMove(game, in, out, 1);
								}
								if (result == -1) { // local error in insertion gives you another try
									continue;
								}
								System.out.print(game); // at this point the game is worth printing
								// 1 - player 1 wins
								if (result == 1) {
									byte[] winMsg = {'Y','O','U',' ','W','I','N'};
									out.write(winMsg);
									out.flush();
									haventPlayedYet = false;
									System.out.println("You lose!");
									break;
								}
								// -2 - draw
								if (result == -2) {
									byte[] errMsg = {'E','R','R','O','R'};
									out.write(errMsg);
									out.flush();
									haventPlayedYet = false;
									System.out.println("The game is a draw");
									break;
								}
								// 3 - signal to end game sent from waitForMove
								// could have been a game win or an error
								if (result == 3) {
									break;
								}
								yourTurn ^= true; // swapping turn with a XOR
							}
						}
					}
				}
				else { // no message found, send your own
					Random over9000 = new Random();
					int thisClientPort = 9000 + over9000.nextInt(100);

					String portStr = Integer.toString(thisClientPort);
					byte[] portBytes = portStr.getBytes();
					byte[] newGame = {'N','E','W',' ','G','A','M','E',':', portBytes[0], portBytes[1], portBytes[2], portBytes[3]};
					DatagramPacket outPacket = new DatagramPacket(newGame, newGame.length, dAdd, broadcastPort);
					socket.send(outPacket);
					try (
						ServerSocket ss = new ServerSocket(thisClientPort);
						BufferedReader terminalInput = new BufferedReader(new InputStreamReader(System.in))
					) {
						ss.setSoTimeout(10000); // wait 10 seconds before moving on to listening
						Socket s2 = ss.accept();
						InputStream in = s2.getInputStream();
						OutputStream out = s2.getOutputStream();

						Connect4 game = new Connect4();
						boolean yourTurn = true; // player 1
						while (true) {
							int result = -1;
							if (yourTurn) {
								result = makeMove(game, out, terminalInput, 1);
							}
							else { // other player's turn
								result = waitForMove(game, in, out, 2);
							}
							// -1 - local error in insertion gives you another try
							if (result == -1) {
								continue;
							}
							// 2 - player 2 wins
							if (result == 2) {
								byte[] winMsg = {'Y','O','U',' ','W','I','N'};
								out.write(winMsg);
								out.flush();
								haventPlayedYet = false;
								System.out.println("You lose!");
								break;
							}
							System.out.print(game);
							// -2 - draw
							if (result == -2) {
								byte[] errMsg = {'E','R','R','O','R'};
								out.write(errMsg);
								out.flush();
								haventPlayedYet = false;
								System.out.println("The game is a draw");
								break;
							}
							// 3 - signal to end game sent from waitForMove
							// could have been a game win or an error
							if (result == 3) {
								break;
							}
							yourTurn ^= true; // swapping turn with a XOR
						}
					} catch (SocketTimeoutException e)
					{
						System.out.println("TCP listening timed out, now listening");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Game completed, closing now");
	}
	private static int makeMove(Connect4 game, OutputStream out, BufferedReader terminalInput, int player) {
		try {
			int result = -1;
			System.out.println("Input the column of your move");
			String input = terminalInput.readLine();
			if (!Pattern.matches("^[1-7]$", input)) { // chatGPT used to set up the regex
				System.out.println("Illegal input, write only 1 int in [1,7]");
				return result;
			}
			int column = Integer.parseInt(input);
			result = game.move(column, player);
			byte[] outMsg = {'I','N','S','E','R','T',':',(byte) ('0'+column)}; // converting the digit to a char
			out.write(outMsg);
			out.flush();
			//System.out.println("Sent: "+new String(outMsg, StandardCharsets.UTF_8));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	private static int waitForMove(Connect4 game, InputStream in, OutputStream out, int player) {
		try {
			byte[] buffer = new byte[9];
			System.out.println("Waiting for opponent's move");
			Arrays.fill(buffer, (byte) 0);
			in.read(buffer);

			String inMsg = new String(buffer, StandardCharsets.UTF_8);
			//System.out.println("Received: " + inMsg);
			// I don't like these substring functions but idk
			// how else to fix "YOU WIN4" overflow on last byte
			if("YOU WIN".equals(inMsg.substring(0,7)) || "ERROR".equals(inMsg.substring(0,5))) {
				System.out.println(inMsg);
				haventPlayedYet = false;
			    return 3; // end game signal
			}
			// if receiving an invalid input, declare error
			char column = inMsg.charAt(7);
			if (!Pattern.matches("^[1-7]$", Character.toString(column))) {
				byte[] errMsg = {'E','R','R','O','R'};
				out.write(errMsg);
				out.flush();
				haventPlayedYet = false;
				System.out.println("Sent: ERROR");
				return 3; // end game signal
			}
			// return the result of their move
			return game.move(column-'0', player);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
}
