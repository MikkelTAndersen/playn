package playn.android;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import playn.core.Net;
import playn.core.PlayN;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


public class AndroidRTCPeerConnection implements Net.RTCPeerConnection {
	DataChannel dataChannel;
	private PeerConnection pc;
	private Listener listener;
//	private MediaStream localStream;
	private PeerConnectionFactory factory = new PeerConnectionFactory();

	public AndroidRTCPeerConnection(final Listener listener) {
		pc = factory.createPeerConnection(getIceServers(), getConstraints(), new CreatePeerConnectionObserver());

//		localStream = factory.createLocalMediaStream("PLAYN_NS");
//		localStream.addTrack(factory.createAudioTrack("PLAYN_NS2", factory.createAudioSource(constraints)));
//		pc.addStream(localStream, getConstraints());
		this.listener = listener;
	}

	private List<IceServer> getIceServers() {
		LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
	    iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
	    iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
	    iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
	    iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
	    iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
        return iceServers;
	}

	private MediaConstraints getConstraints() {
		MediaConstraints constraints = new MediaConstraints();
		PlayN.log().error("OfferToReceiveAudio: true");
		constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		PlayN.log().error("OfferToReceiveVideo: false");
		constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
		PlayN.log().error("DtlsSrtpKeyAgreement: true");
		constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        PlayN.log().error("RtpDataChannels: false");
		constraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels ", "false"));
		return constraints;
	}

	@Override
	public void close() {
		pc.close();
	}

	public RTCDataChannel createOffer() {
		//TODO figure out how to make it sync with HTML reliable true.
		dataChannel = pc.createDataChannel("sendDataCh"+PlayN.random()*10000000, new Init());
		pc.createOffer(new OfferObserver(), getConstraints());
		return new DataChannelEventImpl(dataChannel).getChannel();
	}

	@Override
	public String getLocalDescription() {
		SessionDescription localDescription = pc.getLocalDescription();
		return localDescription.description;
	}

	@Override
	public void createAnswer(String sdp) {
		final SessionDescription sessionDescription = new SessionDescription(Type.OFFER, preferISAC(sdp));
		pc.setRemoteDescription(new RemoteDescriptionObserver(), sessionDescription);
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
	public void setLocalDescription(final String sdp) {
//		RTCSessionDescriptionInit descriptionInit = WebRTC
//				.createRTCSessionDescriptionInit(RTCSdpType.OFFER,
//						sessionDescription);
//		RTCSessionDescription sdp = WebRTC
//				.createRTCSessionDescription(descriptionInit);
		SessionDescription sessionDescription = new SessionDescription(Type.OFFER, preferISAC(sdp));
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
	public void setRemoteDescription(final String sdp) {
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
							byte[] data = new byte[buffer.data.remaining()];
						    buffer.data.get(data);
						    final String dataString = new String(data);
							PlayN.invokeLater(new Runnable() {
								@Override
								public void run() {
									dataListener.onMessage(dataString);
								}
							});
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

	// Mangle SDP to prefer ISAC/16000 over any other audio codec.
	private static String preferISAC(String sdpDescription) {
		String[] lines = sdpDescription.split("\r\n");
		int mLineIndex = -1;
		String isac16kRtpMap = null;
		Pattern isac16kPattern = Pattern
				.compile("^a=rtpmap:(\\d+) ISAC/16000[\r]?$");
		for (int i = 0; (i < lines.length)
				&& (mLineIndex == -1 || isac16kRtpMap == null); ++i) {
			if (lines[i].startsWith("m=audio ")) {
				mLineIndex = i;
				continue;
			}
			Matcher isac16kMatcher = isac16kPattern.matcher(lines[i]);
			if (isac16kMatcher.matches()) {
				isac16kRtpMap = isac16kMatcher.group(1);
				continue;
			}
		}
		if (mLineIndex == -1) {
			Log.d(AndroidRTCPeerConnection.class.getName(), "No m=audio line, so can't prefer iSAC");
			return sdpDescription;
		}
		if (isac16kRtpMap == null) {
			Log.d(AndroidRTCPeerConnection.class.getName(), "No ISAC/16000 line, so can't prefer iSAC");
			return sdpDescription;
		}
		String[] origMLineParts = lines[mLineIndex].split(" ");
		StringBuilder newMLine = new StringBuilder();
		int origPartIndex = 0;
		// Format is: m=<media> <port> <proto> <fmt> ...
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(origMLineParts[origPartIndex++]).append(" ");
		newMLine.append(isac16kRtpMap);
		for (; origPartIndex < origMLineParts.length; ++origPartIndex) {
			if (!origMLineParts[origPartIndex].equals(isac16kRtpMap)) {
				newMLine.append(" ").append(origMLineParts[origPartIndex]);
			}
		}
		lines[mLineIndex] = newMLine.toString();
		StringBuilder newSdpDescription = new StringBuilder();
		for (String line : lines) {
			newSdpDescription.append(line).append("\r\n");
		}
		return newSdpDescription.toString();
	}

	class CreatePeerConnectionObserver implements Observer {
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
			if (iceCandidate.sdp != null) {
				listener.onIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid,
						iceCandidate.sdpMLineIndex);
			}
		}

		@Override
		public void onIceConnectionChange(IceConnectionState iceConnectionState) {
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

		@Override
		public void onRenegotiationNeeded() {
		}
	}

	class OfferObserver implements SdpObserver {
		@Override
		public void onSetSuccess() {
		}

		@Override
		public void onSetFailure(String error) {
			PlayN.log().error("OFFER FAILED : " + error);
		}

		@Override
		public void onCreateSuccess(final SessionDescription sessionDescription) {
			pc.setLocalDescription(new LocalDescriptionObserver(), sessionDescription);
		}

		@Override
		public void onCreateFailure(String error) {
			PlayN.log().error("OFFER FAILED : " + error);
		}
	}

	class LocalDescriptionObserver implements SdpObserver {
		@Override
		public void onSetSuccess() {
		}

		@Override
		public void onSetFailure(String error) {
			PlayN.log().error("OFFER FAILED : " + error);
		}

		@Override
		public void onCreateSuccess(SessionDescription sessionDescription) {
			listener.onSetLocalDescription(sessionDescription.description);
		}

		@Override
		public void onCreateFailure(String error) {
			PlayN.log().error("OFFER FAILED : " + error);
		}
	}

	class RemoteDescriptionObserver implements SdpObserver {
		@Override
		public void onSetSuccess() {
			PlayN.log().error("setRemoteDescription ANSWER onSetSuccess!!!");
			pc.createAnswer(new CreateAnswerObserver(), getConstraints());
		}

		@Override
		public void onSetFailure(String error) {
			PlayN.log().error("setRemoteDescription ANSWER FAILED : " + error);
		}

		@Override
		public void onCreateSuccess(SessionDescription arg0) {
			PlayN.log().error("setRemoteDescription ANSWER onCreateSuccess");
		}

		@Override
		public void onCreateFailure(String error) {
			PlayN.log().error("setRemoteDescription ANSWER CREATE FAILED : " + error);
		}
	}

	class CreateAnswerObserver implements SdpObserver {
		@Override
		public void onSetSuccess() {
			PlayN.log().error("createAnswer ANSWER onSetSuccess");
		}

		@Override
		public void onSetFailure(String error) {
			PlayN.log().error("createAnswer ANSWER FAILED : " + error);
		}

		@Override
		public void onCreateSuccess(final SessionDescription sessionDescription) {
			PlayN.log().error("createAnswer ANSWER onCreateSuccess");
			pc.setLocalDescription(new SdpObserver() {
				@Override
				public void onSetSuccess() {
					PlayN.log().error("setLocalDescription ANSWER onSetSuccess");
					listener.onSetRemoteDescription(sessionDescription.description);
				}

				@Override
				public void onSetFailure(String error) {
					PlayN.log().error("setLocalDescription ANSWER FAILED : " + error);
				}

				@Override
				public void onCreateSuccess(SessionDescription sessionDescription) {
					PlayN.log().error("setLocalDescription ANSWER onCreateSuccess");
				}

				@Override
				public void onCreateFailure(String error) {
					PlayN.log().error("setLocalDescription ANSWER FAILED : " + error);
				}
			}, sessionDescription);
		}

		@Override
		public void onCreateFailure(String error) {
			PlayN.log().error("ANSWER FAILED : " + error);
		}
	}
}
