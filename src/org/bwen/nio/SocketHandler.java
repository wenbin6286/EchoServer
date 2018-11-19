package org.bwen.nio;


import javax.xml.crypto.KeySelector;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketHandler implements IHandler {
    private final IMux mux ;
    private final SocketChannel channel;

    private final ByteBuffer buffer ;
    public SocketHandler(IMux mux, SocketChannel conn) {
        this.mux = mux;
        channel = conn;
        buffer = ByteBuffer.allocate(1024);
    }

    @Override
    public void handleRead() {



        int n;
        try {
            //suspend read
            mux.unregister(this, SelectionKey.OP_READ);
            n = channel.read(buffer);
            System.out.printf("Read %d bytes %n",n);
            if(n <0) {
                close();
                return;
            }
            //have data to write
            String s = buffer.toString();
            if("bye".equalsIgnoreCase(s)) {
                close();
                return;
            }
            mux.register(this, SelectionKey.OP_WRITE);

        } catch (IOException e) {
            e.printStackTrace();

        }

    }
    private  void close() {

        try {
            System.out.println("Closing socket "+getSocketChannel().getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mux.deregister(this);
        return;
    }
    @Override
    public void handleWrite() {

        try {
            mux.unregister(this, SelectionKey.OP_WRITE);
            buffer.flip();
            System.out.printf("writing %d bytes %n", buffer.remaining());
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();//ready for read
            mux.register(this, SelectionKey.OP_READ);
        }
        catch(IOException e) {
            e.printStackTrace();
            }
    }


    @Override
    public void start() {

        try {
            mux.register(this, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SocketChannel getSocketChannel() {
        return channel;
    }
}
