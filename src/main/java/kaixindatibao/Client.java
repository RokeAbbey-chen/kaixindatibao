package kaixindatibao;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    private static final String HOST = "question-miniapp.cmcm.com";
    private static final String TOKEN = "5c90a22c-50e8f103-6ccd548bbaa9b3e5";
    private static final String UID = "1357443331";
    private static final String VERSION = "v2";

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
                                        .addLast(new SslHandler(context.newEngine(ch.alloc())))
                                        .addLast(new ChannelOutboundHandlerAdapter(){
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
        bootstrap.connect(new InetSocketAddress("question-miniapp.cmcm.com", 443))
                .sync()
                .addListener((ChannelFutureListener) future -> {
                    System.out.println("connected finished");
//                    future.channel().writeAndFlush(sessQuestion("c5e2496761ed4e989104be53c1908858"));
//                    future.channel().writeAndFlush(sessGet());
//                    future.channel().writeAndFlush(baseInfo());
                    future.channel().writeAndFlush(wxLogin());
                });
    }

    public static void main(String[] args) throws InterruptedException, SSLException {
        new Client();
    }

    private void setCommonHeaders(FullHttpRequest request){
        request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "utf-8");
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, "gzip");
        request.headers().set(HttpHeaders.Names.REFERER, "https://servicewechat.com/wx8d94eabe7f62ffd0/21/page-frame.html");
        request.headers().set(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (Linux; Android 9; MI 8 SE Build/PKQ1.181121.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/70.0.3538.110 Mobile Safari/537.36 MicroMessenger/7.0.3.1400(0x2700033B) Process/appbrand0 NetType/WIFI Language/zh_CN");
        request.headers().set(HttpHeaders.Names.HOST, HOST);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE); // or HttpHeaders.Values.CLOSE
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        request.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json");

    }

    private FullHttpRequest wxLogin(){
        String uri = "https://question-miniapp.cmcm.com/question/user/wxlogin";
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                uri
        );
        setCommonHeaders(request);
//        ByteBuf buf = Unpooled.copiedBuffer("{\"common\":{\"uid\":null,\"token\":null},\"jscode\":\"033S879E0gX76l2SYY8E054N8E0S879f\",\"inviter_uid\":\"\"}", CharsetUtil.UTF_8);
        ByteBuf buf = Unpooled.copiedBuffer("{\"common\":{\"uid\":null,\"token\":null},\"jscode\":\"033SKSYc22IY6F0WGoZc2qlAYc2SKSYk\",\"inviter_uid\":\"\"}", CharsetUtil.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
        request.content().clear().writeBytes(buf);
        return request;
    }

    private FullHttpRequest baseInfo(){
        String uri = "https://question-miniapp.cmcm.com/question/anss/base_info";
        FullHttpRequest request =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                        HttpMethod.POST,
                        uri
                );
        setCommonHeaders(request);
        ByteBuf bbuf = Unpooled.copiedBuffer(
                "{\"common\":{\"token\":\"5c90ab28-50e8f103-58b66369934d6d62\",\"uid\":\"1357443331\"},\"timestamp\":1552984998281,\"version\":\"v2\"}"
//                "{\"common\":{\"token\":\"5c90a22c-50e8f103-6ccd548bbaa9b3e5\",\"uid\":\"1357443331\"},\"version\":\"v2\"}"
                , StandardCharsets.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);
        return request;

    }

    private FullHttpRequest sessGet(){
        String uri = "https://question-miniapp.cmcm.com/question/anss/sess_get";
        FullHttpRequest request =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                        HttpMethod.POST,
                        uri
                );
        setCommonHeaders(request);
        ByteBuf bbuf = Unpooled.copiedBuffer(
                "{\"common\":{\"token\":\" " + TOKEN + " \",\"uid\":\"" + UID + "\"},\"version\":\"" + VERSION + "\"}"
//                "{\"common\":{\"token\":\"5c90a22c-50e8f103-6ccd548bbaa9b3e5\",\"uid\":\"1357443331\"},\"version\":\"v2\"}"
                , StandardCharsets.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);
        return request;
    }

    private FullHttpRequest sessQuestion(String sessId){
        String uri = "https://question-miniapp.cmcm.com/question/anss/sess_question";
        FullHttpRequest request =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                        HttpMethod.POST,
                        uri
                );
        setCommonHeaders(request);
//        ByteBuf bbuf = Unpooled.copiedBuffer("{\"common\":{\"token\":\"" + TOKEN + "\",\"uid\":\"" + UID + "\"},\"sess_id\":\"" + sessId
//                        + "\",\"version\":\"" + VERSION + "\"}"
//                , StandardCharsets.UTF_8);
        ByteBuf bbuf = Unpooled.copiedBuffer("{\"common\":{\"token\":\"5c90ab28-50e8f103-58b66369934d6d62\",\"uid\":\"1357443331\"},\"sess_id\":\"c5e2496761ed4e989104be53c1908858\",\"version\":\"v2\"}"
                , StandardCharsets.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);
        return request;

    }


    private static class MyHandler extends SimpleChannelInboundHandler<FullHttpResponse> implements ChannelOutboundHandler {
        private int status = STATUS_GET;
        private static final int STATUS_BASE = 10;
        private static final int STATUS_GET = STATUS_BASE;
        private static final int STATUS_QUESTION = STATUS_GET + 1;
        private static final Pattern SESS_GET_PATTERN = Pattern.compile("sess_id.*?:.*?\"(.*?)\"");
//        "sess_id": "c5e2496761ed4e989104be53c1908858",
        private Client client;
        public MyHandler(Client client){
            this.client = client;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            System.out.println("read from server:");
            String output = msg.content().toString(CharsetUtil.UTF_8);
            System.out.println(output);
            FullHttpRequest request = null;
            switch (status){
                case STATUS_GET + 999:
                    Matcher matcher = SESS_GET_PATTERN.matcher(output);
                    if (matcher.find()){
                        String sessId = matcher.group(1);
                        request = client.sessQuestion(sessId);
                    }
                    break;
                default:System.out.println("nothing to do");break;
            }
            if (request != null) {
                ctx.channel().writeAndFlush(request);
                status ++;
            }
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            ctx.bind(localAddress, promise);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            ctx.connect( remoteAddress, localAddress, promise);
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
