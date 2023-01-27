package common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static common.ServerCommands.SEPARATOR;

public interface ChannelDataExchanger {

    default String readMessage(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        String s;
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append ((char) buffer.get());
        }
        s = sb.toString();
        if (s.endsWith(SEPARATOR))
            s = s.substring(0, s.length()-SEPARATOR.length());
        return s;
    }

    default String readHeader(SocketChannel socketChannel, int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        String s;
        socketChannel.read(buffer);
        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append ((char) buffer.get());
        }
        s = sb.toString();
        return s;
    }

    default String readInfo(SocketChannel socketChannel) throws IOException{
        String str = "";
        while (!str.endsWith(SEPARATOR)) {
            str += readHeader(socketChannel, 1);
        }
        return str.substring(0, str.length() - SEPARATOR.length());
    }

    default void sendMessage(SocketChannel socketChannel, String ... str){
        String message;
        StringBuilder sb = new StringBuilder();
        for (String s: str) {
            sb.append(s).append(SEPARATOR);
        }
        message = sb.toString();
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        buffer.rewind();
        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            System.out.println("Unable to send");
            e.printStackTrace();
        }
        buffer.clear();
    }
}
