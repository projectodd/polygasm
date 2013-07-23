package org.projectodd.vertx.jgroups.integration;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

public class JGroupsMultiClusterTest extends TestVerticle {

    private int responsesSeen = 0;

    @Override
    public void start() {

        initialize();
        // container.deployModule(System.getProperty("vertx.modulename"), 2, new AsyncResultHandler<String>() {
        container.deployVerticle(JGroupsTestVerticle.class.getName(),
                new JsonObject()
                        .putString("cluster", "tacos"),
                2,
                new AsyncResultHandler<String>() {
                    @Override
                    public void handle(AsyncResult<String> asyncResult) {
                        System.err.println("2 deployed: " + asyncResult.result());
                        VertxAssert.assertTrue(asyncResult.succeeded());
                        if (!asyncResult.succeeded()) {
                            asyncResult.cause().printStackTrace();
                        }
                        VertxAssert.assertNotNull("deploymentID should not be null", asyncResult.result());
                        container.deployVerticle(JGroupsTestVerticle.class.getName(),
                                new JsonObject()
                                        .putString("cluster", "potatoes"),
                                2,
                                new AsyncResultHandler<String>() {
                                    @Override
                                    public void handle(AsyncResult<String> arg0) {
                                        VertxAssert.assertTrue(arg0.succeeded());
                                        System.err.println("2 deployed: " + arg0.result());
                                        if (!arg0.succeeded()) {
                                            arg0.cause().printStackTrace();
                                        }
                                        VertxAssert.assertNotNull("deploymentID should not be null", arg0.result());
                                        startTests();
                                    }

                                });

                    }
                });
    }

    @Test
    public void testClustering() throws Exception {
        vertx.eventBus().registerHandler("test.tacos.responses", new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> event) {
                VertxAssert.assertEquals(2, event.body().size());
                ++responsesSeen;
                if (responsesSeen == 4) {
                    VertxAssert.testComplete();
                }
            }
        });
        
        vertx.eventBus().registerHandler("test.potatoes.responses", new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> event) {
                VertxAssert.assertEquals(2, event.body().size());
                ++responsesSeen;
                if (responsesSeen == 4) {
                    VertxAssert.testComplete();
                }
            }
        });

        vertx.eventBus().publish("test.tacos", true);
        vertx.eventBus().publish("test.potatoes", true);
    }

}
