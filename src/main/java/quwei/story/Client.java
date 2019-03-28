package quwei.story;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import util.CharacterUtil;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {


    public Client() throws InterruptedException, SSLException {
        Bootstrap bootstrap = new Bootstrap();
        OioEventLoopGroup group = new OioEventLoopGroup();
        bootstrap.group(group)
                .channel(OioSocketChannel.class)
                .handler(
                        new ChannelInitializer<Channel>() {
                            SslContext context = SslContextBuilder.forClient().build();

                            @Override
                            protected void initChannel(Channel ch) throws Exception {
                                ch.pipeline()
//                                        .addLast(new SslHandler(context.newEngine(ch.alloc())))
                                        .addLast(new ChannelOutboundHandlerAdapter() {
                                            @Override
                                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                                System.out.println("http output:==============================================================================================================");
                                                ByteBuf byteBuf = (ByteBuf) msg;
                                                System.out.println(byteBuf.toString(CharsetUtil.UTF_8));
                                                ctx.writeAndFlush(msg, promise);
                                            }
                                        })
                                        .addLast(new HttpRequestEncoder())
                                        .addLast(new HttpResponseDecoder())
                                        .addLast(new HttpObjectAggregator(512 * 1024))
                                        .addLast(new MyHandler(Client.this));
                            }
                        }
                );
//        https://question-miniapp.cmcm.com/question/anss/sess_question
//        bootstrap.connect(new InetSocketAddress("192.168.1.48", 12345))
        bootstrap.connect(new InetSocketAddress("m.beauty-story.cn", 80))
                .sync()
                .addListener((ChannelFutureListener) future -> {
                    System.out.println("connected finished");
                    future.channel().writeAndFlush(questionsRequest("3241240"));
                });
    }

    public static void main(String[] args) throws InterruptedException, SSLException {
        new Client();
    }

    private void setCommonHeaders(FullHttpRequest request) {

//        POST /api/paper/iv2 HTTP/1.1
//User-Agent: Mozilla/5.0 (Linux; Android 9; MI 8 SE Build/PKQ1.181121.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/70.0.3538.110 Mobile Safari/537.36
//Content-Type: application/x-www-form-urlencoded
//Accept-Encoding: gzip, deflate
//Accept-Language: zh-CN,en-US;q=0.9
//X-Requested-With: com.HanBinLi.Paper
//Connection: keep-alive

        request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, "gzip, deflate");
        request.headers().set(HttpHeaders.Names.ACCEPT_LANGUAGE, "zh-CN,en-US;q=0.9");
        request.headers().set("X-Requested-With", "com.HanBinLi.Paper");
//        request.headers().set(HttpHeaders.Names.REFERER, "https://servicewechat.com/wx8d94eabe7f62ffd0/21/page-frame.html");
        request.headers().set(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (Linux; Android 9; MI 8 SE Build/PKQ1.181121.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/70.0.3538.110 Mobile Safari/537.36");
        request.headers().set(HttpHeaders.Names.HOST, "m.beauty-story.cn");
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE); // or HttpHeaders.Values.CLOSE
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        request.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
        request.headers().add(HttpHeaders.Names.ACCEPT, "application/json, text/plain, */*");
        request.headers().add(HttpHeaders.Names.ORIGIN, "file://");

    }

    private FullHttpRequest questionsRequest(String id) {
        // "m.beauty-story.cn"
        String uri = "/api/paper/iv2";
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                uri
        );
        setCommonHeaders(request);
        ByteBuf buf = Unpooled.copiedBuffer("id=" + id, CharsetUtil.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
        request.content().clear().writeBytes(buf);
        return request;

    }

    private static class MyHandler extends SimpleChannelInboundHandler<FullHttpResponse> implements ChannelOutboundHandler {
        private int status = STATUS_GET;
        private static final int STATUS_BASE = 10;
        private static final int STATUS_GET = STATUS_BASE;
        private static final int STATUS_QUESTION = STATUS_GET + 1;
        private static final Pattern HASHID_PATTERN = Pattern.compile("\"hashid\":\"(\\d+)\"");
        private Client client;


        public MyHandler(Client client) {
            this.client = client;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            System.out.println("read from server:");
            String output = msg.content().toString(CharsetUtil.UTF_8);
            String result = CharacterUtil.plainText2UnicodeText(output);
            Matcher m = HASHID_PATTERN.matcher(result);
            System.out.println(result);
            if (m.find(result.length() - 50)){
                String id = m.group(1);
                ctx.channel().writeAndFlush(client.questionsRequest(id));
            } else {
                System.out.println("结束");
            }

        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            ctx.bind(localAddress, promise);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            ctx.connect(remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            ctx.disconnect(promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            ctx.close(promise);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            ctx.deregister(promise);
        }

        @Override
        public void read(ChannelHandlerContext ctx) throws Exception {
            ctx.read();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ctx.writeAndFlush(msg, promise);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }
    }
}
