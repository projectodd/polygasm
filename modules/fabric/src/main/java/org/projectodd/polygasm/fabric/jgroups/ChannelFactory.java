package org.projectodd.polygasm.fabric.jgroups;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.protocols.BARRIER;
import org.jgroups.protocols.FD;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.MERGE2;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.RSVP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST2;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE;
import org.jgroups.stack.Protocol;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class ChannelFactory {

    private Container container;
    private Vertx vertx;
    private String cluster;
    private String uuid;

    public ChannelFactory(Container container, Vertx vertx, String cluster) {
        this.container = container;
        this.vertx = vertx;
        this.cluster = cluster;
        this.uuid = UUID.randomUUID().toString();
    }

    public Channel newChannel() throws Exception {
        JChannel channel = new JChannel(getProtocols());
        return channel;
    }

    protected List<Protocol> getProtocols() {
        List<Protocol> protocols = new ArrayList<>();

        protocols.add(createTransport());
        //protocols.add( new TRACE( uuid ) );
        protocols.add(createPing());
        protocols.add(createMerge());
        protocols.add(createFailureDetection());
        protocols.add(createVerifySuspect());
        protocols.add(createBarrier());
        protocols.add(createNakAck());
        protocols.add(createUnicast());
        protocols.add(createStable());
        protocols.add(createGMS());
        protocols.add(createUFC());
        protocols.add(createMFC());
        protocols.add(createFragmentation());
        protocols.add(createRSVP());
        protocols.add(createState());

        return protocols;
    }

    protected Protocol createState() {
        STATE state = new STATE();
        return state;
    }

    protected Protocol createTransport() {

        VertxTransportProtocol vertxProto = new VertxTransportProtocol(this.vertx, uuid, this.cluster );
        vertx.sharedData().getMap("org.jgroups.vertx.proto").put( uuid, vertxProto  );
        
        container.deployWorkerVerticle(ReceiverVerticle.class.getName(), new JsonObject().putString("uuid", uuid).putString("cluster", this.cluster));
        return vertxProto;
    }

    protected Protocol createPing() {
        PING ping = new PING();
        ping.setNumInitialMembers(2);
        ping.setTimeout(6000);
        return ping;
    }

    protected Protocol createMerge() {
        MERGE2 merge = new MERGE2();
        return merge;
    }

    protected Protocol createFailureDetection() {
        FD fd = new FD();
        return fd;
    }

    protected Protocol createVerifySuspect() {
        VERIFY_SUSPECT verify = new VERIFY_SUSPECT();
        return verify;

    }

    protected Protocol createBarrier() {
        BARRIER barrier = new BARRIER();
        return barrier;
    }

    protected Protocol createNakAck() {
        NAKACK2 nakAck = new NAKACK2();
        return nakAck;
        /*
         * this.id=8;
         * xmit_interval = 1000;
         * xmit_table_num_rows = 100;
         * xmit_table_msgs_per_row = 10000;
         * xmit_table_max_compaction_time = 10000;
         * max_msg_batch_size = 100;
         */
    }

    protected Protocol createUnicast() {
        UNICAST2 unicast = new UNICAST2();

        return unicast;
        /*
         * this.id=9;
         * stable_interval = 5000;
         * xmit_interval = 500;
         * max_bytes = 1024;
         * xmit_table_num_rows = 20;
         * xmit_table_msgs_per_row = 10000;
         * xmit_table_max_compaction_time = 10000;
         * max_msg_batch_size = 100;
         * conn_expiry_timeout = 0;
         */
    }

    protected Protocol createStable() {
        STABLE stable = new STABLE();
        return stable;
        /*
         * this.id=10;
         * stability_delay = 500;
         * desired_avg_gossip = 5000;
         * max_bytes = 1024;
         */
    }

    protected Protocol createGMS() {
        GMS gms = new GMS();
        return gms;
        /*
         * this.id=11;
         * setPrintLocalAddr(false);
         * setJoinTimeout(3000);
         * setViewBundling(true);
         */
    }

    protected Protocol createUFC() {
        UFC ufc = new UFC();
        return ufc;
        /*
         * this.id=12;
         * max_credits = 200000;
         * min_threshold = 0.20;
         */
    }

    protected Protocol createMFC() {
        MFC mfc = new MFC();
        return mfc;
        /*
         * this.id=13;
         * max_credits = 200000;
         * min_threshold = 0.20;
         */
    }

    protected Protocol createFragmentation() {
        FRAG2 frag = new FRAG2();
        return frag;
        /*
         * frag_size = 8000;
         */
    }

    protected Protocol createRSVP() {
        RSVP rsvp = new RSVP();
        return rsvp;
        /*
         * timeout = 60000;
         * resend_interval = 500;
         * ack_on_delivery = false;
         */
    }
}
