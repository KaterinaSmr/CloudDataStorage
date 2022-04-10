package common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MyObjectOutputStream implements ServerCommands{
    private SocketChannel socketChannel;
    private ObjectOutputStream outStream;
    private ByteArrayOutputStream bAOutputStream;

    public MyObjectOutputStream(SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;
        this.bAOutputStream = new ByteArrayOutputStream(256);
        this.outStream = new ObjectOutputStream(bAOutputStream);
    }

    public void writeObject(Object ob) throws IOException {
        outStream.writeObject(ob);
        byte[] arr = bAOutputStream.toByteArray();
//        System.out.println(Arrays.toString(arr));
//        System.out.println("Количество байт " + arr.length);

        int maxLeading0s = COMMAND_LENGTH - FILES_TREE.length() - 1;
        ByteBuffer buffer0 = ByteBuffer.wrap((FILES_TREE + " "
                + String.format("%0" + maxLeading0s + "d", arr.length)).getBytes());
        //формирование сообщения вида "/ftree 00081"

        buffer0.rewind();
        socketChannel.write(buffer0);
        //отправка сообщения "/ftree 00081" с информацией о кол-ве байт в объекте

        ByteBuffer buffer = ByteBuffer.wrap(arr);
        buffer.rewind();
        socketChannel.write(buffer);
    }

    public void close() throws IOException{
        outStream.close();
    }
}
