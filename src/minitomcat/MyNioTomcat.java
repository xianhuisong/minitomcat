package minitomcat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyNioTomcat {
    private int port = 8080;
    private Map<String, String> urlServletMap = new HashMap<>();

    ExecutorService taskPool = Executors.newCachedThreadPool();

    public void start() throws IOException {

        initServletMapping();
        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.bind(new InetSocketAddress(port));

        System.out.println("mini nio tomcat start");

        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {

            int readyChannels = selector.select();

            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();

            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {

                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();
                    if (socketChannel != null) {
                        System.out.println("收到了来自：" + ((InetSocketAddress) socketChannel.getRemoteAddress()).getHostString() + "的请求");
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }

                } else if (key.isReadable()) {

                    SocketChannel socketChannel = (SocketChannel) key.channel();

                    String requestHeader = null;
                    try {
                        requestHeader = receive(socketChannel);
                    } catch (Exception e) {
                        System.out.println("读取socketChannel出错");
                        return;
                    }
                    if (requestHeader.length() > 0) {
//                        System.out.println("该请求的头格式为:" + requestHeader);
//                        StringBuffer bufferResponse = new StringBuffer();
//
//                        bufferResponse.append("HTTP/1.1 200 OK\r\n")
//                                .append("Server: localhost" + "\r\n")
//                                .append("Content-Type: text/html\r\n")
//                                .append("\r\n")
//                                .append("<html><head><title>SHOW</title></head><body>")
//                                .append("test")
//                                .append("</body></html>");
//                        ByteBuffer buffer = ByteBuffer.allocate(1024);
//                        buffer.put(bufferResponse.toString().getBytes());
//                        buffer.flip();
                        //socketChannel.configureBlocking(false);
                        //socketChannel.register(key.selector(), SelectionKey.OP_WRITE);
//                        socketChannel.write(buffer);
//                        socketChannel.close();
                        taskPool.submit(new HttpHandler(socketChannel, key, requestHeader));
                    }


                }
//                else if (key.isWritable()) {
//                    SocketChannel socketChannel = (SocketChannel) key.channel();
//                    socketChannel.shutdownInput();
//                    socketChannel.close();
//                }
                iterator.remove();
            }

        }


    }

    class HttpHandler implements Runnable {
        private SocketChannel socketChannel;
        private SelectionKey key;
        private String header;

        public HttpHandler(SocketChannel socketChannel, SelectionKey key, String header) {
            this.socketChannel = socketChannel;
            this.key = key;
            this.header = header;
        }


        @Override
        public void run() {
            try {

                System.out.println(header);
                String httpHead = header.split("\n")[0];
                String url = httpHead.split("\\s")[1];
                String method = httpHead.split("\\s")[0];

                MyRequest request = new MyRequest(url, method);
                MyResponse response = new MyResponse(socketChannel, key);

                String clazz = urlServletMap.get(request.getUrl());
                Class<MyServlet> myServletClass = (Class<MyServlet>) Class.forName(clazz);
                MyServlet myServlet = myServletClass.newInstance();
                myServlet.service(request, response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void initServletMapping() {
        for (ServletMapping servletMapping : ServletMappingConfig.servletMappingList) {
            urlServletMap.put(servletMapping.getUrl(), servletMapping.getClazz());
        }

    }

    private String receive(SocketChannel socketChannel) throws Exception {
        //声明一个1024大小的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] bytes = null;
        int size = 0;
        //定义一个字节数组输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //将socketChannel中的数据写入到buffer中，此时的buffer为写模式，size为写了多少个字节
        while ((size = socketChannel.read(buffer)) > 0) {
            //将写模式改为读模式
            //The limit is set to the current position and then the position is set to zero.
            //将limit设置为之前的position，而将position置为0，更多java nio的知识会写成博客的
            buffer.flip();
            bytes = new byte[size];
            //将Buffer写入到字节数组中
            buffer.get(bytes);
            //将字节数组写入到字节缓冲流中
            baos.write(bytes);
            //清空缓冲区
            buffer.clear();
        }
        //将流转回字节数组
        bytes = baos.toByteArray();
        return new String(bytes);
    }

    public static void main(String[] args) throws IOException {
        new MyNioTomcat().start();
    }
}
