package org.projectodd.polygasm.fabric;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class ServiceClient {

    private Vertx vertx;

    public ServiceClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public void provision(String address, String deploymentId, JsonObject config, Handler<Message<JsonObject>> replyHandler) {
        this.vertx.eventBus().send(address + ".provision", new JsonObject()
                .putString("id", deploymentId)
                .putObject("config", config), replyHandler);
    }
    
    public void unprovision(String address, String deploymentId, JsonObject config, Handler<Message<JsonObject>> replyHandler) {
        this.vertx.eventBus().send(address + ".unprovision", new JsonObject()
                .putString("id", deploymentId),
                replyHandler );
    }

}
