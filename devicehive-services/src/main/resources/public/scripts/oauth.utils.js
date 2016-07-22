var encodeBase64 = function (data) {
    var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var o1, o2, o3, h1, h2, h3, h4, bits, i = 0, ac = 0, enc = "", tmp_arr = [];
    if (!data) {
        return data;
    }
    do { // pack three octets into four hexets
        o1 = data.charCodeAt(i++);
        o2 = data.charCodeAt(i++);
        o3 = data.charCodeAt(i++);
        bits = o1 << 16 | o2 << 8 | o3;
        h1 = bits >> 18 & 0x3f;
        h2 = bits >> 12 & 0x3f;
        h3 = bits >> 6 & 0x3f;
        h4 = bits & 0x3f;

        // use hexets to index into b64, and append result to encoded string
        tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
    } while (i < data.length);
    enc = tmp_arr.join('');
    var r = data.length % 3;
    return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);
};

var getUrlParam = function (name) {
    return decodeURIComponent(((RegExp(name + '=' + '(.+?)(&|$)', 'i').exec(location.search) || [, ""
    ])[1]).replace(/\+/g, ' '));
};

var callDeviceHive = function (serviceUrl, login, password, method, url, params) {
    var settings = {
        type: method,
        url: serviceUrl + url,
        dataType: "json",
        data: params,
        //context: self,
        xhrFields: { withCredentials: true },
        beforeSend: function (jqXHR, settings) {
            if (login && password) {
                jqXHR.setRequestHeader("Authorization", "Basic " + encodeBase64(login + ":" + password))
            }
        }
    };
    if (params && (method == "POST" || method == "PUT")) {
        settings.contentType = "application/json";
        settings.data = JSON.stringify(params);
    }
    return $.ajax(settings);
};