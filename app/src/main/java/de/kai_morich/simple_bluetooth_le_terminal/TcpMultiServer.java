package de.kai_morich.simple_bluetooth_le_terminal;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class TcpMultiServer implements Runnable {
       protected int          serverPort   = 8080;
        protected ServerSocket serverSocket = null;
        protected boolean      isStopped    = false;
        protected Thread       runningThread= null;
        protected String       name;
        protected boolean      global;
        private static final String TAG = "TcpMultiServer";
        public abstract void newclient(Socket clientSocket);

        public TcpMultiServer(int port, String name) {
                this(port, name, false);
        }

        public TcpMultiServer(int port, String name, boolean global) {
            this.serverPort = port;
            this.name = name;
            this.global = global;
        }

        public void run() {
                synchronized(this) {
                        this.runningThread = Thread.currentThread();
                }
                try {
                        if (global)
                                this.serverSocket = new ServerSocket(this.serverPort);
                        else                    /* listen only on 127.0.0.1 */
                                this.serverSocket = new ServerSocket(this.serverPort, 0,
                                                                     InetAddress.getByName(null));

                } catch (IOException e) {
                        throw new RuntimeException("Cannot open port " + this.serverPort, e);
                }
                Log.d(TAG, name + " started @Port: " + this.serverPort);
                while(! isStopped()){
                        Socket clientSocket = null;
                        try {
                                clientSocket = this.serverSocket.accept();
                        } catch (IOException e) {
                                if(isStopped()) {
                                        Log.d(TAG,"Stopped.");
                                        return;
                                }
                                throw new RuntimeException(
                                                           "Error accepting client connection", e);
                        }
                        this.newclient(clientSocket);
                }
                Log.d(TAG,  name + " Stopped.");
                while(! isStopped()){
                        Socket clientSocket = null;
                        try {
                                clientSocket = this.serverSocket.accept();
                        } catch (IOException e) {
                                if(isStopped()) {
                                        Log.d(TAG,"Stopped.");
                                        return;
                                }
                                throw new RuntimeException(
                                                           "Error accepting client connection", e);
                        }
                        this.newclient(clientSocket);
                }
                Log.d(TAG,name + " Stopped.");
        }

        private synchronized boolean isStopped() {
                return this.isStopped;
        }

        public synchronized void stop(){
                this.isStopped = true;
                try {
                        this.serverSocket.close();
                } catch (IOException e) {
                        throw new RuntimeException("Error closing server", e);
                }
        }

}
