package playn.html;

import playn.core.Net;
import playn.core.PlayN;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.seanchenxi.gwt.html.client.event.CloseEvent;
import com.seanchenxi.gwt.html.client.event.DataChannelEvent;
import com.seanchenxi.gwt.html.client.event.ErrorEvent;
import com.seanchenxi.gwt.html.client.event.IceCandidateEvent;
import com.seanchenxi.gwt.html.client.event.MessageEvent;
import com.seanchenxi.gwt.html.client.event.MessageEvent.Handler;
import com.seanchenxi.gwt.webrtc.client.Constraint;
import com.seanchenxi.gwt.webrtc.client.Constraints;
import com.seanchenxi.gwt.webrtc.client.WebRTC;
import com.seanchenxi.gwt.webrtc.client.connection.RTCConfiguration;
import com.seanchenxi.gwt.webrtc.client.connection.RTCIceCandidate;
import com.seanchenxi.gwt.webrtc.client.connection.RTCIceCandidateInit;
import com.seanchenxi.gwt.webrtc.client.connection.RTCIceServer;
import com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection;
import com.seanchenxi.gwt.webrtc.client.connection.RTCSdpType;
import com.seanchenxi.gwt.webrtc.client.connection.RTCSessionDescription;
import com.seanchenxi.gwt.webrtc.client.connection.RTCSessionDescriptionCallback;
import com.seanchenxi.gwt.webrtc.client.connection.RTCSessionDescriptionInit;
import com.seanchenxi.gwt.webrtc.client.data.DataChannel;

public class HTMLRTCPeerConnection implements Net.RTCPeerConnection {
	DataChannel dataChannel;
	private RTCPeerConnection pc;
	private Listener listener;
	private Constraints constraints;

	public HTMLRTCPeerConnection(final Listener listener) {
        Constraints constraints = getPCConstraints();
        pc = WebRTC.createRTCPeerConnection(getPCConfiguration(), constraints);
		this.listener = listener;
		this.constraints = constraints;
		pc.addDataChannelHandler(new DataChannelEvent.Handler() {
			@Override
			public void onDataChannel(DataChannelEvent event) {
				final DataChannel gwtChannel = event.getChannel();
				HTMLRTCPeerConnection.this.dataChannel = gwtChannel;
				listener.onDataChannel(new RTCDataChannelEventImpl(gwtChannel));
			}
		});
		pc.addIceCandidateHandler(new IceCandidateEvent.Handler() {
			@Override
			public void onIceCandidate(IceCandidateEvent event) {
				RTCIceCandidate candidate = event.getCandidate();
				if(candidate != null) {
					listener.onIceCandidate(candidate.getCandidate(),candidate.getSdpMid(), candidate.getSdpMLineIndex());
				}
			}
		});
	}

    protected RTCConfiguration getPCConfiguration() {
        JsArray<RTCIceServer> iceServers = JavaScriptObject.createArray().cast();
        if (WebRTC.isGecko()) {
            iceServers.push(WebRTC.createRTCIceServer("stun:stun.services.mozilla.com"));
        } else if (WebRTC.isWebkit()) {
        	iceServers.push(WebRTC.createRTCIceServer("stun:stun.l.google.com:19302"));
        	iceServers.push(WebRTC.createRTCIceServer("stun:stun1.l.google.com:19302"));
        	iceServers.push(WebRTC.createRTCIceServer("stun:stun2.l.google.com:19302"));
        	iceServers.push(WebRTC.createRTCIceServer("stun:stun3.l.google.com:19302"));
        	iceServers.push(WebRTC.createRTCIceServer("stun:stun4.l.google.com:19302"));
        }
        return WebRTC.createRTCConfiguration(iceServers);
    }

	protected Constraints getPCConstraints() {
        final Constraints constraints = Constraints.create();
		Constraint dtls = Constraint.create();

		PlayN.log().error("CONSTRAINT_OPTIONAL_DTLSSRTPKEYAGREEMENT: true");
		dtls.set(com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection.CONSTRAINT_OPTIONAL_DTLSSRTPKEYAGREEMENT, true);
        constraints.getOptional().push(dtls);

        Constraint rtpdatachannels = Constraint.create();
        PlayN.log().error("CONSTRAINT_OPTIONAL_RTPDATACHANNELS: false");
        rtpdatachannels.set(com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection.CONSTRAINT_OPTIONAL_RTPDATACHANNELS, false);
        constraints.getOptional().push(rtpdatachannels);

        Constraint mandatory = Constraint.create();
        mandatory.set(com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection.CONSTRAINT_MANDATORY_OFFERTORECEIVEAUDIO, true);
        mandatory.set(com.seanchenxi.gwt.webrtc.client.connection.RTCPeerConnection.CONSTRAINT_MANDATORY_OFFERTORECEIVEVIDEO, false);
        constraints.setMandatory(mandatory);
        return constraints;
    }

	@Override
	public void close() {
		pc.close();
	}

	public RTCDataChannel createOffer() {
		dataChannel = pc.createDataChannel("sendDataCh"+PlayN.random()*10000000,
				WebRTC.createDataChannelInit(true));
		pc.createOffer(new RTCSessionDescriptionCallback() {
			@Override
			public void onSuccess(RTCSessionDescription sessionDescription) {
				hackToFixChromeTransmitSizeIssue(sessionDescription);
				pc.setLocalDescription(sessionDescription);
				listener.onSetLocalDescription(sessionDescription.getSdp());
//				PlayN.log().error("OFFER COMPLETED");
			}


			@Override
			public void onError(String error) {
				PlayN.log().error("OFFER FAILED : " + error);
			}
		}, constraints);
		return new RTCDataChannelEventImpl(dataChannel).getChannel();
	}

	@Override
	public String getLocalDescription() {
		RTCSessionDescription localDescription = pc.getLocalDescription();
		return localDescription.getSdp();
	}

	@Override
	public void createAnswer(String sdp) {
		RTCSessionDescriptionInit descriptionInit = WebRTC
				.createRTCSessionDescriptionInit(RTCSdpType.OFFER, sdp);
		final RTCSessionDescription sessionDesc = WebRTC
				.createRTCSessionDescription(descriptionInit);
		pc.setRemoteDescription(sessionDesc);
		PlayN.invokeLater(new Runnable() {
			@Override
			public void run() {
				pc.createAnswer(new RTCSessionDescriptionCallback() {
					@Override
					public void onSuccess(RTCSessionDescription sessionDescription) {
						hackToFixChromeTransmitSizeIssue(sessionDescription);
						pc.setLocalDescription(sessionDescription);
						listener.onSetRemoteDescription(sessionDescription.getSdp());
//						PlayN.log().error("ANSWER COMPLETED");
					}

					@Override
					public void onError(String error) {
						PlayN.log().error("ANSWER FAILED : " + error);
					}
				}, constraints);
			}
		});
	}

	private void hackToFixChromeTransmitSizeIssue(RTCSessionDescription sessionDescription) {
		String sdp = sessionDescription.getSdp();
		String[] splitted = sdp.split("b=AS:30");
		if(splitted != null && splitted[1] != null) {
			String newSDP = splitted[0] + "b=AS:1638400" + splitted[1];
			sessionDescription.setSdp(newSDP);
		}
	}

	@Override
	public void addIceCandidate(String candidate, String sdpMid,
			int sdpMLineIndex) {
		if (candidate != null) {
//			PlayN.log().error(candidate + " " + sdpMid + " " + sdpMLineIndex);
			RTCIceCandidateInit iceCandidateInit = WebRTC
					.createRTCIceCandidateInit(candidate, sdpMid, sdpMLineIndex);
			RTCIceCandidate iceCandidate = WebRTC
					.createRTCIceCandidate(iceCandidateInit);
			if (iceCandidate != null) {
				pc.addIceCandidate(iceCandidate);
			}
		}
	}

	@Override
	public void setLocalDescription(String sessionDescription) {
		RTCSessionDescriptionInit descriptionInit = WebRTC
				.createRTCSessionDescriptionInit(RTCSdpType.OFFER,
						sessionDescription);
		RTCSessionDescription sdp = WebRTC
				.createRTCSessionDescription(descriptionInit);
		pc.setLocalDescription(sdp);
	}

	@Override
	public String getRemoteDescription() {
		return pc.getRemoteDescription().getSdp();
	}

	@Override
	public void setRemoteDescription(String sessionDescription) {
		RTCSessionDescriptionInit descriptionInit = WebRTC
				.createRTCSessionDescriptionInit(RTCSdpType.ANSWER,
						sessionDescription);
		RTCSessionDescription sdp = WebRTC
				.createRTCSessionDescription(descriptionInit);
		pc.setRemoteDescription(sdp);
	}

	private class RTCDataChannelEventImpl implements RTCDataChannelEvent {
		private final DataChannel gwtChannel;
		private RTCDataChannel rtcDataChannel;

		public RTCDataChannelEventImpl(DataChannel channel) {
			this.gwtChannel = channel;
		    rtcDataChannel = new RTCDataChannel(){
				@Override
				public void addListener(final Listener dataListener) {
//					PlayN.log().error("ADDED LISTENERS " + gwtChannel.getReadyState());
					gwtChannel.addMessageHandler(new Handler() {
						@Override
						public void onMessage(MessageEvent event) {
//							PlayN.log().error("onMessage LISTENERS");
							dataListener.onMessage((String) event.getData());
						}
					});
					gwtChannel.addCloseHandler(new CloseEvent.Handler() {
						@Override
						public void onClose(CloseEvent event) {
							dataListener.onClose();
						}
					});
					gwtChannel.addOpenHandler(new com.seanchenxi.gwt.html.client.event.OpenEvent.Handler(){
						@Override
						public void onOpen(com.seanchenxi.gwt.html.client.event.OpenEvent event) {
//							PlayN.log().error("addOpenHandler LISTENERS");
							dataListener.onOpen();
						}
					});
					gwtChannel.addErrorHandler(new ErrorEvent.Handler<NativeEvent>(){
						@Override
						public void onError(ErrorEvent<NativeEvent> event) {
//							PlayN.log().error("onError LISTENERS " +event.toDebugString());
							dataListener.onError(event.toDebugString());
						}});
				}

				@Override
				public void send(String data) {
					gwtChannel.send(data);
				}

				@Override
				public void close() {
					gwtChannel.close();
				}

				@Override
				public RTCDataChannelState getState() {
					switch (gwtChannel.getReadyState()) {
					case CONNECTING:
						return RTCDataChannelState.CONNECTING;
					case CLOSING:
						return RTCDataChannelState.CLOSING;
					case OPEN:
						return RTCDataChannelState.OPEN;
					case CLOSED:
						return RTCDataChannelState.CLOSED;
					}
					return RTCDataChannelState.CLOSED;
				}
			};
		}

		@Override
		public RTCDataChannel getChannel() {
			return rtcDataChannel;
		}
	}
}
