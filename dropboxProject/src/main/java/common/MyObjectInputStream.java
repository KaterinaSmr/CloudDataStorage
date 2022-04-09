package common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MyObjectInputStream {
    private SocketChannel socketChannel;

    public MyObjectInputStream(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
    }

    public Object readObject(int objectBytesSize) throws IOException, ClassNotFoundException{
        byte[] recArr = new byte[objectBytesSize];
        ByteBuffer buffer = ByteBuffer.allocate(objectBytesSize);
        socketChannel.read(buffer);
        buffer.flip();
        int i = 0;
        while (buffer.hasRemaining()) {
            recArr[i++] = buffer.get();
        }
        ByteArrayInputStream bAInputStream = new ByteArrayInputStream(recArr);
        ObjectInputStream inStream = new ObjectInputStream(bAInputStream);

        Object obj = inStream.readObject();
        inStream.close();

        return obj;
    }

}
