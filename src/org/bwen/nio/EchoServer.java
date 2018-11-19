package org.bwen.nio;

import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;


import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class EchoServer implements  Runnable, AutoCloseable,IMux{


    private Selector selector ;
    private volatile boolean alive = false;
    private Map<IHandler,SelectionKey> keyMap;
    public EchoServer( int port) throws IOException {

        selector = Selector.open();
        ServerSocketChannel socketListener =  ServerSocketChannel.open();
        socketListener.socket().bind( new InetSocketAddress(port));
        socketListener.configureBlocking(false);
        socketListener.register(selector, SelectionKey.OP_ACCEPT);
        keyMap = new ConcurrentHashMap<>();

    }
    public void run() {
        alive = true;
        while(alive) {
            try {
               int n = selector.select(1000);
               if(n==0) continue;
               Iterator<SelectionKey> iterator =selector.selectedKeys().iterator();
               while(iterator.hasNext()) {
                   SelectionKey key = iterator.next();
                   if(key.isAcceptable()) {
                      ServerSocketChannel server =
                              (ServerSocketChannel) key.channel();
                       SocketChannel conn = server.accept();
                       conn.configureBlocking(false);
                       System.out.printf("accepted connection from %s%n",conn.getRemoteAddress());
                       IHandler handler = new SocketHandler(this,conn);
                       handler.start();

                   }
                   if(key.isReadable()) {
                     IHandler handler = (IHandler)key.attachment();
                     handler.handleRead();
                   }
                   //key may become invalid after read (closed by peer)
                   if(key.isValid() && key.isWritable()) {
                       IHandler handler = (IHandler)key.attachment();
                       handler.handleWrite();
                   }
                   iterator.remove();
               }

            } catch (IOException e) {
                System.out.println("Time to go!");
                return;
            }
        }
    }
    public void close() throws IOException {
        alive = false;
        if(selector != null) {
            selector.wakeup();
        }
    }
    public static void main(String[] args) {
	// write your code here

        int port = 8888;
        if(args.length>0)
            port=Integer.parseInt(args[0]);
        try(EchoServer server = new EchoServer(port)) {
            Thread t = new Thread(server);
            t.start();
           Console console = System.console();
           while(true) {
               if(console != null) {
                   String input = console.readLine();
                   if ("bye".equalsIgnoreCase(input)) {
                       t.interrupt();
                       break;
                   }
               }
               Thread.sleep(1000);
           }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void register(IHandler handler, int Ops) throws ClosedChannelException {

            SelectionKey key = keyMap.get(handler);
            if(key==null) {//new connection
                System.out.println("Register new handler");
                key = handler.getSocketChannel().register(selector, Ops);
                key.attach(handler);
                keyMap.put(handler, key);
            }
            else {
                key.interestOps(key.interestOps() | Ops);
                key.selector().wakeup();
            }
            System.out.println(key.interestOps());


    }
    @Override
    public void unregister(IHandler handler, int Ops) {
        SelectionKey key = keyMap.get(handler);
        Objects.requireNonNull(key);
        key.interestOps(key.interestOps() & ~Ops);
        key.selector().wakeup();
        System.out.println("after unregister "+key.interestOps());

    }
    public void deregister(IHandler handler) {

        keyMap.remove(handler);
    }
}
