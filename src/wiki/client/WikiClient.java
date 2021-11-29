package wiki.client;

import com.google.flatbuffers.FlatBufferBuilder;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import wiki.models.base.Type;
import wiki.models.item.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class WikiClient {

    public WikiClient(Channel channel) {
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
