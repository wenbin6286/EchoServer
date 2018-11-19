package org.bwen.nio;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

public interface IMux {
    void register(IHandler handler, int Ops) throws ClosedChannelException;
    void unregister(IHandler handler, int Ops) throws ClosedChannelException;
    void deregister(IHandler handler);
}
