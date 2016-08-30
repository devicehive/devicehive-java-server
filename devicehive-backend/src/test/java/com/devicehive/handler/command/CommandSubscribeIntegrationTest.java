package com.devicehive.handler.command;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandSubscribeRequest;
import com.devicehive.model.rpc.CommandSubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CommandSubscribeIntegrationTest extends AbstractSpringTest {

    @Autowired
    private RpcClient client;

    @Test
    public void shouldSubscribeToDeviceCommands() throws Exception {
        String device1 = randomUUID().toString();
        String device2 = randomUUID().toString();

        String subscriber1 = randomUUID().toString();
        String subscriber2 = randomUUID().toString();
        String subscriber3 = randomUUID().toString();

        CommandSubscribeRequest sr1 = new CommandSubscribeRequest(subscriber1, device1, null, null);
        Request r1 = Request.newBuilder().withBody(sr1).withSingleReply(false).build();
        TestCallback c1 = new TestCallback();
        client.call(r1, c1);

        CommandSubscribeRequest sr2 = new CommandSubscribeRequest(subscriber1, device2,
                Collections.singleton("increase_temperature"), null);
        Request r2 = Request.newBuilder().withBody(sr2).withSingleReply(false).build();
        TestCallback c2 = new TestCallback();
        client.call(r2, c2);

        CommandSubscribeRequest sr3 = new CommandSubscribeRequest(subscriber2, device2, null, null);
        Request r3 = Request.newBuilder().withBody(sr3).withSingleReply(false).build();
        TestCallback c3 = new TestCallback();
        client.call(r3, c3);

        CommandSubscribeRequest sr4 = new CommandSubscribeRequest(subscriber2, device1,
                Collections.singleton("toggle_lights"), null);
        Request r4 = Request.newBuilder().withBody(sr4).withSingleReply(false).build();
        TestCallback c4 = new TestCallback();
        client.call(r4, c4);

        CommandSubscribeRequest sr5 = new CommandSubscribeRequest(subscriber3, randomUUID().toString(), null, null);
        Request r5 = Request.newBuilder().withBody(sr5).withSingleReply(false).build();
        TestCallback c5 = new TestCallback();
        client.call(r5, c5);

        //wait for subscribers to subscribe
        Stream.of(c1.subscribeFuture, c2.subscribeFuture, c3.subscribeFuture, c4.subscribeFuture, c5.subscribeFuture)
                .forEach(CompletableFuture::join);

        //devices send commands
        List<CompletableFuture<Response>> futures = Stream.of(device1, device2).flatMap(device -> {
            List<CompletableFuture<Response>> list = Stream.of("increase_temperature", "toggle_lights").map(name -> {
                DeviceCommand command = new DeviceCommand();
                command.setId(0);
                command.setCommand(name);
                command.setDeviceGuid(device);
                CommandInsertRequest event = new CommandInsertRequest(command);
                CompletableFuture<Response> f = new CompletableFuture<>();
                client.call(Request.newBuilder().withBody(event).build(), f::complete);
                return f;
            }).collect(Collectors.toList());
            return list.stream();
        }).collect(Collectors.toList());

        //wait for commands to be delivered
        futures.forEach(CompletableFuture::join);

        assertThat(c1.commands, hasSize(2));
        c1.commands.forEach(event -> {
            assertNotNull(event.getCommand());
            assertEquals(event.getCommand().getDeviceGuid(), device1);
            assertEquals(event.getCommand().getId(), Long.valueOf(0));
        });
        Set<String> names = c1.commands.stream()
                .map(n -> n.getCommand().getCommand())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("increase_temperature", "toggle_lights"));

        assertThat(c2.commands, hasSize(1));
        CommandEvent e = c2.commands.stream().findFirst().get();
        assertNotNull(e.getCommand());
        assertEquals(e.getCommand().getDeviceGuid(), device2);
        assertEquals(e.getCommand().getId(), Long.valueOf(0));
        assertEquals(e.getCommand().getCommand(), "increase_temperature");

        assertThat(c3.commands, hasSize(2));
        c3.commands.forEach(event -> {
            assertNotNull(event.getCommand());
            assertEquals(event.getCommand().getDeviceGuid(), device2);
            assertEquals(event.getCommand().getId(), Long.valueOf(0));
        });
        names = c3.commands.stream()
                .map(n -> n.getCommand().getCommand())
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("increase_temperature", "toggle_lights"));

        assertThat(c4.commands, hasSize(1));
        e = c4.commands.stream().findFirst().get();
        assertNotNull(e.getCommand());
        assertEquals(e.getCommand().getDeviceGuid(), device1);
        assertEquals(e.getCommand().getId(), Long.valueOf(0));
        assertEquals(e.getCommand().getCommand(), "toggle_lights");

        assertThat(c5.commands, is(empty()));
    }

    public static class TestCallback implements Consumer<Response> {

        private CompletableFuture<CommandSubscribeResponse> subscribeFuture;
        private Set<CommandEvent> commands;

        public TestCallback() {
            this.subscribeFuture = new CompletableFuture<>();
            this.commands = new HashSet<>();
        }

        @Override
        public void accept(Response response) {
            if (response.getBody().getAction().equals(Action.COMMAND_SUBSCRIBE_RESPONSE.name())) {
                subscribeFuture.complete((CommandSubscribeResponse) response.getBody());
            } else if (response.getBody().getAction().equals(Action.COMMAND_EVENT.name())) {
                commands.add((CommandEvent) response.getBody());
            } else {
                throw new IllegalArgumentException("Unexpected response " + response);
            }
        }
    }
}
