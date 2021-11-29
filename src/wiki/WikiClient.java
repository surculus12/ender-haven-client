package wiki;

import com.google.flatbuffers.FlatBufferBuilder;
import io.grpc.Channel;
import wiki.Message;
import wiki.models.base.Type;
import wiki.models.item.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WikiClient {
    private static final WikiClient client = new WikiClient();
    private final ConcurrentLinkedQueue<Message> messages;
    private final Set<String> cached;
    private final ScheduledExecutorService exec;

    private WikiClient() {
        messages = new ConcurrentLinkedQueue<>();
        cached = new HashSet<>();
        exec = Executors.newScheduledThreadPool(1);
        // TODO: Perform the initial getcached
        // TODO: Generalise the message cache-hits, ad-hoc updating of
        //       the caching to avoid dupes, and spamming on interval to
        //       avoid socket overhead
        exec.scheduleAtFixedRate(sendMessages, 1, 1, TimeUnit.MINUTES);
    }

    private void sendMessages() throws IOException {
        Socket socket = new Socket("localhost", 50051);
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        output.write(m.encode());

        Message res = Message.read(input);
        IsCachedRes isCachedRes = IsCachedRes.getRootAsIsCachedRes(ByteBuffer.wrap(res.payload));
        System.out.println("Cached?: " + isCachedRes.cached());
    }
    
    public void checkCache(byte[] hash) {

    }

    public static void main(String[] args) throws Exception {
        FlatBufferBuilder builder = new FlatBufferBuilder(2);
        int gobId = builder.createString("test");
        IsCachedReq.startIsCachedReq(builder);
        IsCachedReq.addGobid(builder, gobId);
        int root = IsCachedRes.endIsCachedRes(builder);
        builder.finish(root);
        byte[] payload = builder.sizedByteArray();
        for (byte b : payload) {
            System.out.print(Byte.toUnsignedInt(b));
            System.out.print(" ");
        }
        System.out.println();

        Socket socket = new Socket("localhost", 50051);
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        Message req = new Message(Type.ItemIsCached, payload);
        output.write(req.encode());

        Message res = Message.read(input);
        IsCachedRes isCachedRes = IsCachedRes.getRootAsIsCachedRes(ByteBuffer.wrap(res.payload));
        System.out.println("Cached?: " + isCachedRes.cached());
    }
}
