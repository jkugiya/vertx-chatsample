package jp.kugiya.chatsample;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * 
 * @author jiro
 *
 */
public class ChatServer extends Verticle {

  private static final String MESSAGE_BROADCAST_ADDRESS = "broadcast_address";

  public void start() {
    NetServer netserver = vertx.createNetServer();
    netserver.connectHandler((NetSocket socket) -> {
      InetSocketAddress remoteAddr = socket.remoteAddress();
      String addr = String.format("%s:%d", remoteAddr.getAddress(), remoteAddr.getPort());
      socket.write(String.format("Welcome to the chat %s!", addr));
      // コネクション1つにつき1つのハンドラを登録する。
      Handler<Message<String>> messageHandler = (event) -> {
        socket.write(event.body());
      };
      vertx.eventBus().registerHandler(MESSAGE_BROADCAST_ADDRESS, messageHandler);
      // ソケットからメッセージを受け取ったときにEventBusを通じてメッセージをpublishする。
      socket.dataHandler((Buffer data) -> {
        ZonedDateTime now = ZonedDateTime.now();
        String msg = String.format("%s<%s>: %s", now, addr, data);
        vertx.eventBus().publish(MESSAGE_BROADCAST_ADDRESS, msg);
      });
      // コネクションが閉じられたらEventBusから閉じられたコネクションのメッセージハンドラを取り除く。
      socket.closeHandler((Void event) -> {
        vertx.eventBus().unregisterHandler(MESSAGE_BROADCAST_ADDRESS, messageHandler);
      });
    });
    netserver.listen(1234);

  }
}
