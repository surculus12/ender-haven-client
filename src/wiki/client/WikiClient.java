package wiki.client;

import com.google.flatbuffers.FlatBufferBuilder;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import wiki.models.item.CheckCacheReq;
import wiki.models.item.CheckCacheRes;
import wiki.models.item.ItemGrpc;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class WikiClient {
    private Channel channel;
    ItemGrpc.ItemBlockingStub stub;
    public WikiClient(Channel channel) {
	this.channel = channel;
	this.stub = ItemGrpc.newBlockingStub(channel);
    }
    
    public void checkCache(byte[] hash) {
	FlatBufferBuilder builder = new FlatBufferBuilder(hash.length);
	int hashOffset = builder.createByteVector(hash);
	int reqOffset = CheckCacheReq.createCheckCacheReq(builder, hashOffset);
	ByteBuffer buf = ByteBuffer.wrap(builder.sizedByteArray(0, reqOffset));
	CheckCacheReq req = CheckCacheReq.getRootAsCheckCacheReq(buf);
	CheckCacheRes res = stub.checkCache(req);
	System.out.println(res.cached());
    }
    
    public static void main(String[] args) throws Exception {
	String target = "localhost:50051";
	ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
	    // TODO: Set up ssl
	    .usePlaintext()
	    .build();
	try {
	    WikiClient client = new WikiClient(channel);
	    byte[] hash = new byte[]{0x01, 0x02};
	    client.checkCache(hash);
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
	    // resources the channel should be shut down when it will no longer be used. If it may be used
	    // again leave it running.
	    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
	}
    }
}
