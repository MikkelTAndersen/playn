/**
 * Copyright 2010 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playn.core.Net.RTCPeerConnection.Listener;
import playn.core.NetImpl;
import playn.core.PlayN;
import playn.core.util.Callback;

import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.seanchenxi.gwt.html.client.event.OpenEvent;
import com.seanchenxi.gwt.html.client.event.StateChangeEvent;
import com.seanchenxi.gwt.html.client.util.Json;
import com.seanchenxi.gwt.webrtc.client.Constraint;
import com.seanchenxi.gwt.webrtc.client.Constraints;
import com.seanchenxi.gwt.webrtc.client.WebRTC;

public class HtmlNet extends NetImpl {

  public HtmlNet(HtmlPlatform platform) {
    super(platform);
  }

  @Override
  public WebSocket createWebSocket(String url, WebSocket.Listener listener) {
    return new HtmlWebSocket(url, listener);
  }

  @Override
  protected void execute(final BuilderImpl req, final Callback<Response> callback) {
    try {
      XMLHttpRequest xhr = XMLHttpRequest.create();
      xhr.open(req.method(), req.url);
      for (Header header : req.headers) {
        xhr.setRequestHeader(header.name, header.value);
      }
      xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
        @Override
        public void onReadyStateChange(final XMLHttpRequest xhr) {
          if (xhr.getReadyState() == XMLHttpRequest.DONE) {
            callback.onSuccess(new StringResponse(xhr.getStatus(), xhr.getResponseText()) {
              @Override
              protected Map<String,List<String>> extractHeaders() {
                Map<String,List<String>> headers = new HashMap<String,List<String>>();
                String block = xhr.getAllResponseHeaders();
                for (String line : block.split("\r\n")) {
                  int cidx = line.indexOf(":");
                  if (cidx > 0) {
                    String name = line.substring(0, cidx);
                    List<String> values = headers.get(name);
                    if (values == null) headers.put(name, values = new ArrayList<String>());
                    values.add(line.substring(cidx+1).trim());
                  }
                }
                return headers;
              }
              @Override
              public String header(String name) {
                // some browsers have buggy implementation of getAllResponseHeaders, so calling
                // this directly instead of relying on our parsed map helps things to mostly work
                // in those cases; yay web!
                return xhr.getResponseHeader(name);
              }
              @Override
              public List<String> headers(String name) {
                // if we were able to parse the headers ourselves, use those, but if not (due
                // perhaps to bugs, etc.) then fall back to using getResponseHeader
                List<String> values = super.headers(name);
                if (!values.isEmpty()) return values;
                String value = xhr.getResponseHeader(name);
                return (value == null) ? values : Collections.singletonList(value);
              }
            });
          }
        }
      });
      if (req.isPost()) {
        if (req.payloadBytes != null) {
          throw new UnsupportedOperationException("Raw bytes not currently supported in HTML5.");
        }
        xhr.setRequestHeader("Content-Type", req.contentType());
        xhr.send(req.payloadString);
      } else {
        xhr.send();
      }
    } catch (Exception e) {
      callback.onFailure(e);
    }
  }

	@Override
	public RTCPeerConnection createRTCPeerConnection(String url, final Listener listener) {
//        JsArray<RTCIceServer> iceServers = JavaScriptObject.createArray().cast();
//        if (WebRTC.isGecko()) {
//            iceServers.push(WebRTC.createRTCIceServer("stun:stun.services.mozilla.com"));
//        } else if (WebRTC.isWebkit()) {
//            iceServers.push(WebRTC.createRTCIceServer("stun:stun.l.google.com:19302"));
//        }
        final com.seanchenxi.gwt.webrtc.client.connection.RTCConfiguration configuration = null;//WebRTC.createRTCConfiguration(iceServers);
        final Constraints constraints = getPCConstraints();
        final com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection pc = WebRTC.createRTCPeerConnection(configuration, constraints);
        PlayN.log().info("Created RTCPeerConnection with:\n  configuration=" + Json.stringify(configuration) + "\n  constraints=" + Json.stringify(constraints) + "\n");

		pc.addOpenHandler(new OpenEvent.Handler() {
			@Override
			public void onOpen(OpenEvent event) {
				PlayN.log().error("OpenEvent : "+ event);
			}
		});

		pc.addStateChangeHandler(new StateChangeEvent.Handler() {
			@Override
			public void onStateChange(StateChangeEvent event) {
				PlayN.log().error("StateChangeEvent : "+ event);
			}
		});

        final HTMLRTCPeerConnection con = new HTMLRTCPeerConnection(pc, listener, constraints);
       return con;
	}

	protected Constraints getPCConstraints() {
        Constraint constraint = Constraint.create();
        constraint.set(com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection.CONSTRAINT_OPTIONAL_RTPDATACHANNELS, true);
        final Constraints constraints = Constraints.create();
        constraints.getOptional().push(constraint);
        return constraints;
    }
}
