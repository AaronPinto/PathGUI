package BrowserStuff;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;

public class WebServer {
	public static void main(String args[]) throws IOException {
		ServerSocket server = new ServerSocket(8000);
		while (true) {
			Socket socket = null;
			try {
				socket = server.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}			
			new Thread(new OutputHandler(socket)).start();
			new Thread(new InputHandler(socket)).start();
		}
	}

	public static class OutputHandler implements Runnable {
		protected Socket s = null;

		public OutputHandler(Socket clientSocket) {
			this.s = clientSocket;
		}

		@Override
		public void run() {
			try {
				String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + 
						readFile("C:\\Users\\aaron\\Webserver\\Webpage.html", StandardCharsets.UTF_8);
				s.getOutputStream().write(httpResponse.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class InputHandler implements Runnable {
		protected Socket s = null;

		public InputHandler(Socket clientSocket) {
			this.s = clientSocket;
		}

		@Override
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				while(br.readLine() != null)
					System.out.println(br.readLine());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}