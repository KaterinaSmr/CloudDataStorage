package common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static common.ServerCommands.SEPARATOR;

public interface ChannelReader  {

    default String readMessage(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        String s = "";
        while (buffer.hasRemaining()) {
            s += (char) buffer.get();
        }
        if (s.endsWith(SEPARATOR))
            s = s.substring(0, s.length()-SEPARATOR.length());
        return s;
    }

    default String readHeader(SocketChannel socketChannel, int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        String s = "";
        socketChannel.read(buffer);
        buffer.flip();
        while (buffer.hasRemaining()) {
            s += (char) buffer.get();
        }
        return s;
    }

    default String readInfo(SocketChannel socketChannel) {
        String str = "";
        try {
            while (!str.endsWith(SEPARATOR)) {
                str += readHeader(socketChannel,1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.substring(0, str.length() - SEPARATOR.length());
    }
}
