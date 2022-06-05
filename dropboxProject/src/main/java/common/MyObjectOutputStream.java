package common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MyObjectOutputStream implements ServerCommands{
    private SocketChannel socketChannel;

    public MyObjectOutputStream(SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;
    }

    public void writeObject(Object ob) throws IOException {
        ByteArrayOutputStream bAOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(bAOutputStream);
        outStream.writeObject(ob);
        byte[] arr = bAOutputStream.toByteArray();
        ByteBuffer buffer0 = ByteBuffer.wrap((arr.length + SEPARATOR).getBytes());
        buffer0.rewind();
        socketChannel.write(buffer0);

        ByteBuffer buffer = ByteBuffer.wrap(arr);
        buffer.rewind();
        socketChannel.write(buffer);
        outStream.close();
    }

}
