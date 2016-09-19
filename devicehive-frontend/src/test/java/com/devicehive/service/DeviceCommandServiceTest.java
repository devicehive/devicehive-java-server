package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.model.wrappers.DeviceCommandWrapper;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceCommandServiceTest extends AbstractResourceTest {

    private static final String DEFAULT_STATUS = "default_status";

    @Autowired
    private DeviceCommandService deviceCommandService;

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Autowired
    private TimestampService timestampService;

    @Mock
    private RequestHandler requestHandler;

    private ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Test
    public void testFindCommandsByGuid() throws Exception {
        final List<String> guids = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        final Date timestampSt = timestampService.getDate();
        final Date timestampEnd = timestampService.getDate();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";

        final Set<String> guidsForSearch = new HashSet<>(Arrays.asList(
                guids.get(0),
                guids.get(2),
                guids.get(3)));

        final Map<String, DeviceCommand> commandMap = guidsForSearch.stream()
                .collect(Collectors.toMap(Function.identity(), guid -> {
                    DeviceCommand command = new DeviceCommand();
                    command.setId(System.nanoTime());
                    command.setDeviceGuid(guid);
                    command.setCommand(RandomStringUtils.randomAlphabetic(10));
                    command.setTimestamp(timestampService.getDate());
                    command.setParameters(new JsonStringWrapper(parameters));
                    command.setStatus(DEFAULT_STATUS);
                    return command;
                }));

        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            String guid = request.getBody().cast(CommandSearchRequest.class).getGuid();
            CommandSearchResponse response = new CommandSearchResponse();
            response.setCommands(Collections.singletonList(commandMap.get(guid)));
            return Response.newBuilder()
                    .withBody(response)
                    .buildSuccess();
        });

        deviceCommandService.find(guidsForSearch, Collections.emptySet(), timestampSt, timestampEnd, DEFAULT_STATUS)
                .thenAccept(commands -> {
                    assertEquals(3, commands.size());
                    assertEquals(new HashSet<>(commandMap.values()), new HashSet<>(commands));
                })
                .get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(3)).handle(argument.capture());
    }

    @Test
    public void testFindCommandsByGuidAndName() throws Exception {
        final List<String> names = IntStream.range(0, 5)
                .mapToObj(i -> RandomStringUtils.randomAlphabetic(10))
                .collect(Collectors.toList());
        final Date timestampSt = timestampService.getDate();
        final Date timestampEnd = timestampService.getDate();
        final String parameters = "{\"param1\":\"value1\",\"param2\":\"value2\"}";
        final String guid = UUID.randomUUID().toString();

        final Set<String> namesForSearch = new HashSet<>(Arrays.asList(
                names.get(0),
                names.get(2),
                names.get(3)));

        final List<DeviceCommand> commandList = namesForSearch.stream()
                .map(name -> {
                    DeviceCommand command = new DeviceCommand();
                    command.setId(System.nanoTime());
                    command.setDeviceGuid(guid);
                    command.setCommand(name);
                    command.setTimestamp(timestampService.getDate());
                    command.setParameters(new JsonStringWrapper(parameters));
                    command.setStatus(DEFAULT_STATUS);
                    return command;
                }).collect(Collectors.toList());

        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            CommandSearchResponse response = new CommandSearchResponse();
            response.setCommands(commandList);
            return Response.newBuilder()
                    .withBody(response)
                    .buildSuccess();
        });

        deviceCommandService.find(Collections.singleton(guid), names, timestampSt, timestampEnd, DEFAULT_STATUS)
                .thenAccept(commands -> {
                    assertEquals(3, commands.size());
                    assertEquals(new HashSet<>(commandList), new HashSet<>(commands));
                })
                .get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void testFindCommand() throws Exception {
        final String guid = UUID.randomUUID().toString();
        final long id = System.nanoTime();

        final DeviceCommand command = new DeviceCommand();
        command.setId(id);
        command.setDeviceGuid(guid);
        command.setCommand(RandomStringUtils.randomAlphabetic(10));
        command.setTimestamp(timestampService.getDate());
        command.setStatus(DEFAULT_STATUS);

        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            CommandSearchResponse response = new CommandSearchResponse();
            response.setCommands(Collections.singletonList(command));
            return Response.newBuilder()
                    .withBody(response)
                    .buildSuccess();
        });

        deviceCommandService.findOne(id, guid)
                .thenAccept(deviceCommand -> assertTrue(deviceCommand.isPresent()))
                .get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void testInsertCommands() throws Exception {
        final int num = 10;
        when(requestHandler.handle(any(Request.class))).then(invocation -> {
            CommandInsertRequest insertRequest = invocation.getArgumentAt(0, Request.class)
                    .getBody().cast(CommandInsertRequest.class);
            return Response.newBuilder()
                    .withBody(new CommandInsertResponse(insertRequest.getDeviceCommand()))
                    .buildSuccess();
        });

        final UserVO user = new UserVO();
        user.setId(System.nanoTime());
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(UserRole.CLIENT);

        final DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(System.nanoTime());
        deviceVO.setGuid(UUID.randomUUID().toString());

        for (int i = 0; i < num; i++) {
            final DeviceCommandWrapper deviceCommand = new DeviceCommandWrapper();
            deviceCommand.setCommand(Optional.of("command" + i));
            deviceCommand.setParameters(Optional.of(new JsonStringWrapper("{'test':'test'}")));
            deviceCommand.setStatus(Optional.of(DEFAULT_STATUS));

            deviceCommandService.insert(deviceCommand, deviceVO, user)
                    .thenAccept(deviceCom -> {
                        assertNotNull(deviceCom);
                        assertNotNull(deviceCom.getId());
                        assertNotNull(deviceCom.getUserId());
                        assertNotNull(deviceCom.getTimestamp());
                    }).get(2, TimeUnit.SECONDS);
        }

        verify(requestHandler, times(num)).handle(argument.capture());
    }

    @Test
    public void testUpdateCommand() throws Exception {
        final DeviceCommand deviceCommand = new DeviceCommand();
        deviceCommand.setId(System.nanoTime());
        deviceCommand.setDeviceGuid(UUID.randomUUID().toString());
        deviceCommand.setCommand("command");
        deviceCommand.setParameters(new JsonStringWrapper("{'test':'test'}"));
        deviceCommand.setStatus(DEFAULT_STATUS);

        final DeviceCommandWrapper commandWrapper = new DeviceCommandWrapper();
        commandWrapper.setStatus(Optional.of("OK"));
        commandWrapper.setLifetime(Optional.of(100500));

        when(requestHandler.handle(any(Request.class))).then(invocation -> Response.newBuilder()
                .buildSuccess());

        deviceCommandService.update(deviceCommand, commandWrapper).
                thenAccept(Assert::assertNull).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

}