package org.projectodd.vertx.jgroups;


import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class ReceiverVerticle extends Verticle implements Handler<Message<JsonObject>> {

    private String uuid;
    private String mcastGroup;
    private VertxTransportProtocol proto;

    @Override
    public void start(Future<Void> startedResult) {
        this.uuid = container.config().getString( "uuid" );
        this.mcastGroup = container.config().getString( "cluster" );
        
        this.proto = (VertxTransportProtocol) vertx.sharedData().getMap("org.jgroups.vertx.proto").get(uuid);
        
        vertx.eventBus().registerHandler("org.jgroups.vertx.multicast." + mcastGroup, this );
        vertx.eventBus().registerHandler("org.jgroups.vertx.unicast." + uuid, this );
        
        startedResult.setResult(null);
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void handle(Message<JsonObject> event) {
        this.proto.receive( event.body() );
    }

}
