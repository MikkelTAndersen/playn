package playn.android;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.webrtc.DataChannel;
import org.webrtc.DataChannel.Buffer;
import org.webrtc.DataChannel.Init;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import playn.core.Net;
import playn.core.PlayN;


public class AndroidRTCPeerConnection implements Net.RTCPeerConnection {
	DataChannel dataChannel;
	private PeerConnection pc;
	private Listener listener;
	private MediaConstraints constraints;

	public AndroidRTCPeerConnection(final Listener listener) {
		PeerConnectionFactory factory = new PeerConnectionFactory();
		pc = factory.createPeerConnection(Collections.<IceServer>emptyList(), getConstraints(), new PeerConnection.Observer(){
			@Override
			public void onAddStream(MediaStream arg0) {
			}

			@Override
			public void onDataChannel(DataChannel dataChannel) {
				AndroidRTCPeerConnection.this.dataChannel = dataChannel;
				listener.onDataChannel(new DataChannelEventImpl(dataChannel));
			}

			@Override
			public void onError() {
			}

			@Override
			public void onIceCandidate(IceCandidate iceCandidate) {
				if(iceCandidate.sdp != null) {
					listener.onIceCandidate(iceCandidate.sdp,iceCandidate.sdpMid, iceCandidate.sdpMLineIndex);
				}
			}

			@Override
			public void onIceConnectionChange(IceConnectionState arg0) {
			}

			@Override
			public void onIceGatheringChange(IceGatheringState arg0) {
			}

			@Override
			public void onRemoveStream(MediaStream arg0) {
			}

			@Override
			public void onSignalingChange(SignalingState arg0) {
			}
		});
		this.listener = listener;
	}

	private MediaConstraints getConstraints() {
		MediaConstraints constraints = new MediaConstraints();
		constraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
		return constraints;
	}

	@Override
	public void close() {
		pc.close();
	}

	public RTCDataChannel createOffer() {
		//TODO figure out how to make it sync with HTML reliable true.
		dataChannel = pc.createDataChannel("sendDataCh", new Init());
		pc.createOffer(new SdpObserver() {
			@Override
			public void onSetSuccess() {
			}

			@Override
			public void onSetFailure(String error) {
				PlayN.log().error("OFFER FAILED : " + error);
			}

			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				pc.setLocalDescription(new SdpObserver() {
					@Override
					public void onSetSuccess() {
					}

					@Override
					public void onSetFailure(String error) {
						PlayN.log().error("OFFER FAILED : " + error);
					}

					@Override
					public void onCreateSuccess(SessionDescription arg0) {
					}

					@Override
					public void onCreateFailure(String error) {
						PlayN.log().error("OFFER FAILED : " + error);
					}
				}, sessionDescription);
				listener.onSetLocalDescription(sessionDescription.description);

			}

			@Override
			public void onCreateFailure(String error) {
				PlayN.log().error("OFFER FAILED : " + error);
			}
		}, constraints);
		return new DataChannelEventImpl(dataChannel).getChannel();
	}

	@Override
	public String getLocalDescription() {
		SessionDescription localDescription = pc.getLocalDescription();
		return localDescription.description;
	}

	@Override
	public void createAnswer(String sdp) {
//		RTCSessionDescriptionInit descriptionInit = WebRTC
//				.createRTCSessionDescriptionInit(RTCSdpType.OFFER, sdp);
//		final RTCSessionDescription sessionDesc = WebRTC
//				.createRTCSessionDescription(descriptionInit);
		SessionDescription sessionDescription = new SessionDescription(Type.OFFER, sdp);
		pc.setRemoteDescription(new SdpObserver() {
			@Override
			public void onSetSuccess() {
			}

			@Override
			public void onSetFailure(String error) {
				PlayN.log().error("ANSWER FAILED : " + error);
			}

			@Override
			public void onCreateSuccess(SessionDescription arg0) {
			}

			@Override
			public void onCreateFailure(String error) {
				PlayN.log().error("ANSWER FAILED : " + error);
			}
		}, sessionDescription);
		pc.createAnswer(new SdpObserver() {
			@Override
			public void onSetSuccess() {
			}

			@Override
			public void onSetFailure(String error) {
				PlayN.log().error("ANSWER FAILED : " + error);
			}

			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				pc.setLocalDescription(new SdpObserver() {

					@Override
					public void onSetSuccess() {
					}

					@Override
					public void onSetFailure(String error) {
						PlayN.log().error("ANSWER FAILED : " + error);
					}

					@Override
					public void onCreateSuccess(SessionDescription arg0) {
					}

					@Override
					public void onCreateFailure(String error) {
						PlayN.log().error("ANSWER FAILED : " + error);
					}
				},sessionDescription);
				listener.onSetRemoteDescription(sessionDescription.description);
			}

			@Override
			public void onCreateFailure(String error) {
				PlayN.log().error("ANSWER FAILED : " + error);
			}
		}, constraints);

	}

	@Override
	public void addIceCandidate(String candidate, String sdpMid,
			int sdpMLineIndex) {
		if (candidate != null) {
//			PlayN.log().error(candidate + " " + sdpMid + " " + sdpMLineIndex);
//			RTCIceCandidateInit iceCandidateInit = WebRTC
//					.createRTCIceCandidateInit(candidate, sdpMid, sdpMLineIndex);
//			RTCIceCandidate iceCandidate = WebRTC
//					.createRTCIceCandidate(iceCandidateInit);
			pc.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, candidate));
		}
	}

	@Override
	public void setLocalDescription(String sdp) {
//		RTCSessionDescriptionInit descriptionInit = WebRTC
//				.createRTCSessionDescriptionInit(RTCSdpType.OFFER,
//						sessionDescription);
//		RTCSessionDescription sdp = WebRTC
//				.createRTCSessionDescription(descriptionInit);
		SessionDescription sessionDescription = new SessionDescription(Type.OFFER, sdp);
		pc.setLocalDescription(new SdpObserver() {

			@Override
			public void onSetSuccess() {
			}

			@Override
			public void onSetFailure(String error) {
				PlayN.log().error("Local Description FAILED : " + error);
			}

			@Override
			public void onCreateSuccess(SessionDescription arg0) {
			}

			@Override
			public void onCreateFailure(String error) {
				PlayN.log().error("Local Description FAILED : " + error);
			}
		}, sessionDescription);
	}

	@Override
	public String getRemoteDescription() {
		return pc.getRemoteDescription().description;
	}

	@Override
	public void setRemoteDescription(String sdp) {
//		RTCSessionDescriptionInit descriptionInit = WebRTC
//				.createRTCSessionDescriptionInit(RTCSdpType.ANSWER,
//						sessionDescription);
//		RTCSessionDescription sdp = WebRTC
//				.createRTCSessionDescription(descriptionInit);
		SessionDescription sessionDescription = new SessionDescription(Type.ANSWER, sdp);

		pc.setRemoteDescription(new SdpObserver() {

			@Override
			public void onSetSuccess() {
			}

			@Override
			public void onSetFailure(String error) {
				PlayN.log().error("Remote Description FAILED : " + error);
			}

			@Override
			public void onCreateSuccess(SessionDescription arg0) {
			}

			@Override
			public void onCreateFailure(String error) {
				PlayN.log().error("Local Description FAILED : " + error);
			}
		}, sessionDescription);
	}

	private class DataChannelEventImpl implements RTCDataChannelEvent {
		private final DataChannel dataChannel;
		private RTCDataChannel rtcDataChannel;

		public DataChannelEventImpl(DataChannel channel) {
			this.dataChannel = channel;
		    rtcDataChannel = new RTCDataChannel(){
				@Override
				public void addListener(final Listener dataListener) {
					dataChannel.registerObserver(new DataChannel.Observer() {
						@Override
						public void onStateChange() {
						}

						@Override
						public void onMessage(Buffer buffer) {
							dataListener.onMessage(new String(buffer.data.array()));
						}
					});

////					PlayN.log().error("ADDED LISTENERS " + gwtChannel.getReadyState());
//					dataChannel.addMessageHandler(new Handler() {
//						@Override
//						public void onMessage(MessageEvent event) {
////							PlayN.log().error("onMessage LISTENERS");
//						}
//					});
//					dataChannel.addCloseHandler(new CloseEvent.Handler() {
//						@Override
//						public void onClose(CloseEvent event) {
//							dataListener.onClose();
//						}
//					});
//					dataChannel.addOpenHandler(new com.seanchenxi.gwt.html.client.event.OpenEvent.Handler(){
//						@Override
//						public void onOpen(com.seanchenxi.gwt.html.client.event.OpenEvent event) {
////							PlayN.log().error("addOpenHandler LISTENERS");
//							dataListener.onOpen();
//						}
//					});
//					dataChannel.addErrorHandler(new ErrorEvent.Handler<NativeEvent>(){
//						@Override
//						public void onError(ErrorEvent<NativeEvent> event) {
////							PlayN.log().error("onError LISTENERS " +event.toDebugString());
//							dataListener.onError(event.toDebugString());
//						}});
				}

				@Override
				public void close() {
					dataChannel.close();
				}

				@Override
				public void send(String data) {
					dataChannel.send(new DataChannel.Buffer(ByteBuffer.wrap(data.getBytes()), false));
				}

				@Override
				public RTCDataChannelState getState() {
					switch (dataChannel.state()) {
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
