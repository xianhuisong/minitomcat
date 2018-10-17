package minitomcat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MyResponse {

    private OutputStream outputStream;

    private SocketChannel socketChannel;

    private SelectionKey key;


    public MyResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public MyResponse(SocketChannel socketChannel, SelectionKey key) {
        this.socketChannel = socketChannel;
        this.key = key;

    }

    public void write(String content) throws IOException {
        StringBuffer bufferResponse = new StringBuffer();
//        http响应协议:
//        HTTP/1.1 200 OK
//        Content-Type: text/html
//
//        <html><body></body></html>

        bufferResponse.append("HTTP/1.1 200 OK\r\n").append("Content-Type: text/html\r\n")
                .append("\r\n")
                .append("<html><body>")
                .append(content)
                .append("</body></html>")
                .append("\r\n");
        write1(bufferResponse.toString());
    }


    public void write1(String content) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        System.out.println("响应：" + content);
        buffer.put(content.getBytes());
        buffer.flip();
        //socketChannel.configureBlocking(false);
        //socketChannel.register(key.selector(), SelectionKey.OP_WRITE);
        socketChannel.write(buffer);
        socketChannel.close();
    }

}
