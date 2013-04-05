/* 
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import com.vaadin.launcher.util.BrowserLauncher;

/**
 * Class for running Jetty servlet container within Eclipse project.
 * 
 */
public class DevelopmentServerLauncher {

    private static final String KEYSTORE = "src/com/vaadin/launcher/keystore";
    private final static int serverPort = 8888;

    /**
     * Main function for running Jetty.
     * 
     * Command line Arguments are passed through to Jetty, see runServer method
     * for options.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        assertAssertionsEnabled();

        // Pass-through of arguments for Jetty
        final Map<String, String> serverArgs = parseArguments(args);
        if (!serverArgs.containsKey("shutdownPort")) {
            serverArgs.put("shutdownPort", "8889");
        }

        int port = Integer.parseInt(serverArgs.get("shutdownPort"));
        if (port > 0) {
            try {
                // Try to notify another instance that it's time to close
                Socket socket = new Socket((String) null, port);
                // Wait until the other instance says it has closed
                socket.getInputStream().read();
                // Then tidy up
                socket.close();
            } catch (IOException e) {
                // Ignore if port is not open
            }
        }

        // Start Jetty
        System.out.println("Starting Jetty servlet container.");
        String url;
        try {
            url = runServer(serverArgs, "Development Server Mode");
            // Start Browser
            if (serverArgs.containsKey("gui") && url != null) {
                System.out.println("Starting Web Browser.");

                // Open browser into application URL
                BrowserLauncher.openBrowser(url);
            }
        } catch (Exception e) {
            // NOP exception already on console by jetty
        }
    }

    private static void assertAssertionsEnabled() {
        try {
            assert false;

            System.err.println("You should run "
                    + DevelopmentServerLauncher.class.getSimpleName()
                    + " with assertions enabled. Add -ea as a VM argument.");
        } catch (AssertionError e) {
            // All is fine
        }
    }

    /**
     * Run the server with specified arguments.
     * 
     * @param serverArgs
     * @return
     * @throws Exception
     * @throws Exception
     */
    protected static String runServer(Map<String, String> serverArgs,
            String mode) throws Exception {

        // Assign default values for some arguments
        assignDefault(serverArgs, "webroot", "WebContent");
        assignDefault(serverArgs, "httpPort", "" + serverPort);
        assignDefault(serverArgs, "context", "");

        int port = serverPort;
        try {
            port = Integer.parseInt(serverArgs.get("httpPort"));
        } catch (NumberFormatException e) {
            // keep default value for port
        }

        // Add help for System.out
        System.out
                .println("-------------------------------------------------\n"
                        + "Starting Vaadin in "
                        + mode
                        + ".\n"
                        + "Running in http://localhost:"
                        + port
                        + "\n-------------------------------------------------\n");

        final Server server = new Server();

        final Connector connector = new SelectChannelConnector();

        connector.setPort(port);
        if (serverArgs.containsKey("withssl")) {
            final SslSocketConnector sslConnector = new SslSocketConnector();
            sslConnector.setPort(8444);
            sslConnector.setTruststore(KEYSTORE);
            sslConnector.setTrustPassword("password");
            sslConnector.setKeystore(KEYSTORE);
            sslConnector.setKeyPassword("password");
            sslConnector.setPassword("password");
            server.setConnectors(new Connector[] { connector, sslConnector });
        } else {
            server.setConnectors(new Connector[] { connector });
        }

        final WebAppContext webappcontext = new WebAppContext();
        String path = DevelopmentServerLauncher.class.getPackage().getName()
                .replace(".", File.separator);
        webappcontext.setContextPath(serverArgs.get("context"));
        webappcontext.setWar(serverArgs.get("webroot"));
        server.setHandler(webappcontext);

        try {
            server.start();

            if (serverArgs.containsKey("shutdownPort")) {
                int shutdownPort = Integer.parseInt(serverArgs
                        .get("shutdownPort"));
                final ServerSocket serverSocket = new ServerSocket(
                        shutdownPort, 1, InetAddress.getByName("127.0.0.1"));
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            System.out
                                    .println("Waiting for shutdown signal on port "
                                            + serverSocket.getLocalPort());
                            // Start waiting for a close signal
                            Socket accept = serverSocket.accept();
                            // First stop listening to the port
                            serverSocket.close();
                            // Then stop the jetty server
                            server.stop();
                            // Send a byte to tell the other process that it can
                            // start jetty
                            OutputStream outputStream = accept
                                    .getOutputStream();
                            outputStream.write(0);
                            outputStream.flush();
                            // Finally close the socket
                            accept.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };

                }.start();

            }
        } catch (Exception e) {
            server.stop();
            throw e;
        }

        return "http://localhost:" + port + serverArgs.get("context");
    }

    /**
     * Assign default value for given key.
     * 
     * @param map
     * @param key
     * @param value
     */
    private static void assignDefault(Map<String, String> map, String key,
            String value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

    /**
     * Parse all command line arguments into a map.
     * 
     * Arguments format "key=value" are put into map.
     * 
     * @param args
     * @return map of arguments key value pairs.
     */
    protected static Map<String, String> parseArguments(String[] args) {
        final Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            final int d = args[i].indexOf("=");
            if (d > 0 && d < args[i].length() && args[i].startsWith("--")) {
                final String name = args[i].substring(2, d);
                final String value = args[i].substring(d + 1);
                map.put(name, value);
            }
        }
        return map;
    }

}
