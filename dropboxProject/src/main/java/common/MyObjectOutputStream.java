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
//        System.out.println(Arrays.toString(arr));
//        System.out.println("Количество байт " + arr.length);

//        int maxLeading0s = COMMAND_LENGTH - FILES_TREE.length() - SEPARATOR.length();
        ByteBuffer buffer0 = ByteBuffer.wrap((FILES_TREE + arr.length + SEPARATOR).getBytes());
        buffer0.rewind();
        socketChannel.write(buffer0);
        //отправка сообщения с информацией о кол-ве байт в объекте

        ByteBuffer buffer = ByteBuffer.wrap(arr);
        buffer.rewind();
        socketChannel.write(buffer);
        outStream.close();
    }

}
