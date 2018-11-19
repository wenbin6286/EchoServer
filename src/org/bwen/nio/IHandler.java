package org.bwen.nio;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface IHandler {

    void handleRead() ;
    void handleWrite();
    void start();
    SocketChannel getSocketChannel();
}
