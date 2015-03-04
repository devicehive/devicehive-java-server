package com.devicehive.service.helpers;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.model.enums.WorkerPath;
import com.devicehive.service.TimestampService;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.devicehive.configuration.Constants.UTF8;

/**
 * Created by tmatvienko on 3/3/15.
 */
@Singleton
public class WorkerUtils {

    @EJB
    private TimestampService timestampService;
    @EJB
    private ConfigurationService configurationService;

    public JsonArray getDataFromWorker(final String commandId, final List<String> deviceGuids, final String names, final String timestamp, final WorkerPath path) throws IOException {
        final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        List<NameValuePair> params = new ArrayList<>();
        if (StringUtils.isNotBlank(commandId)) {
            params.add(new BasicNameValuePair("id", commandId));
        }
        if (deviceGuids != null && !deviceGuids.isEmpty()) {
            params.add(new BasicNameValuePair("deviceGuids", StringUtils.join(deviceGuids, ",")));
        }
        if (StringUtils.isNotBlank(names)) {
            params.add(new BasicNameValuePair("names", names));
        }
        if (StringUtils.isNotBlank(timestamp)) {
            params.add(new BasicNameValuePair("timestamp", timestamp));
        }
        final String paramString = URLEncodedUtils.format(params, Charset.forName(UTF8));
        final GenericUrl url = new GenericUrl(String.format("%s?%s",
                configurationService.get(Constants.CASSANDRA_REST_ENDPOINT) + path.getValue(), paramString));
        final HttpRequest request = requestFactory.buildGetRequest(url);
        return  (JsonArray) new JsonParser().parse(request.execute().parseAsString());
    }
}
