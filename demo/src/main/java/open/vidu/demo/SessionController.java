package open.vidu.demo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.ConnectionType;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.Session;

@Controller
public class SessionController {

	private OpenVidu openVidu; // OpenVidu object as entrypoint of the SDK

	private Map<String, Session> mapSessions = new ConcurrentHashMap<>();
	private Map<String, Map<String, OpenViduRole>> mapSessionNamesTokens = new ConcurrentHashMap<>();

	private String OPENVIDU_URL; // URL where our OpenVidu server is listening
	private String SECRET; // Secret shared with our OpenVidu server

	public SessionController(@Value("${openvidu.secret}") String secret, @Value("${openvidu.url}") String openviduUrl) {
		this.SECRET = secret;
		this.OPENVIDU_URL = openviduUrl;
		this.openVidu = new OpenVidu(OPENVIDU_URL, SECRET);
	}
	
	private Session already; // 미리 방 하나 만들어두고 시작하자
	private String token;
	@PostConstruct
	public void init() {
		ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
				.type(ConnectionType.WEBRTC)
				.role(OpenViduRole.PUBLISHER).data("Already").build();
		
		try {
			already = this.openVidu.createSession();
			token = already.createConnection(connectionProperties).getToken();
			
			this.mapSessions.put("already", already);
			this.mapSessionNamesTokens.put("already", new ConcurrentHashMap<>());
			this.mapSessionNamesTokens.get("already").put(token, OpenViduRole.PUBLISHER);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 

	@RequestMapping(value = "/session", method = RequestMethod.POST)
	public String joinSession(@RequestParam(name = "data") String clientData,
			@RequestParam(name = "session-name") String sessionName, Model model, HttpSession httpSession) {

		try {checkUserLogged(httpSession);} 
		catch (Exception e) { return "index";}

		OpenViduRole role = LoginController.users.get(httpSession.getAttribute("loggedUser")).role;
		String serverData = "{\"serverData\": \"" + httpSession.getAttribute("loggedUser") + "\"}";
	
		ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
														.type(ConnectionType.WEBRTC)
														.role(role).data(serverData).build();

		// System.out.println(connectionProperties.getData()); -> "serverData"
		// System.out.println(connectionProperties.getRole()); -> role
		// System.out.println(connectionProperties.getRtspUri()); -> null
		// System.out.println(connectionProperties.getKurentoOptions()); -> null
		// System.out.println(connectionProperties.getType()); -> WEBRTC
		// System.out.println(connectionProperties.getNetworkCache()); -> null

		if (this.mapSessions.get(sessionName) != null) {
			try {
				String token = this.mapSessions.get(sessionName).createConnection(connectionProperties).getToken();

				System.out.println(sessionName + " 방은 이미 있어 ");
				System.out.println("이 방의 세션은 " + this.mapSessions.get(sessionName) + " 이야");
				System.out.println("너의 토큰 정보는");
				System.out.println(token + " 이고 ");
				System.out.println(" 그 방에 보내줄게 ");

				this.mapSessionNamesTokens.get(sessionName).put(token, role);

				model.addAttribute("sessionName", sessionName);
				model.addAttribute("token", token);
				model.addAttribute("nickName", clientData);
				model.addAttribute("userName", httpSession.getAttribute("loggedUser"));

				return "session";

			} catch (Exception e) {
				model.addAttribute("username", httpSession.getAttribute("loggedUser"));
				return "dashboard";
			}
		} else {
			try {
				Session session = this.openVidu.createSession();
				String token = session.createConnection(connectionProperties).getToken();

				System.out.println("새방 팠다 방 이름은  " + sessionName + " 이야");
				System.out.println("이 방의 세션 " + session + " 이고");
				System.out.println("처음 들어가는 너의 토큰 정보는");
				System.out.println(token + " 이야");
				System.out.println();

				this.mapSessions.put(sessionName, session);
				this.mapSessionNamesTokens.put(sessionName, new ConcurrentHashMap<>());
				this.mapSessionNamesTokens.get(sessionName).put(token, role);

				model.addAttribute("sessionName", sessionName);
				model.addAttribute("token", token);
				model.addAttribute("nickName", clientData);
				model.addAttribute("userName", httpSession.getAttribute("loggedUser"));

				return "session";

			} catch (Exception e) {
				model.addAttribute("username", httpSession.getAttribute("loggedUser"));
				return "dashboard";
			}
		}
	}
	
	@RequestMapping(value = "/leave-session", method = RequestMethod.POST)
	public String removeUser(@RequestParam(name = "session-name") String sessionName,
			@RequestParam(name = "token") String token, Model model, HttpSession httpSession) throws Exception {

//		try { checkUserLogged(httpSession);} 
//		catch (Exception e) {return "index";}
		
		System.out.println("Removing user | sessioName=" + sessionName + ", token=" + token);

		// 해당 이름의 방이 있거나 || 해당 방에 연결된 사람이 있는경우
		if (this.mapSessions.get(sessionName) != null && this.mapSessionNamesTokens.get(sessionName) != null) {

			// 참가자가 아직 있을 경우, remove하면 제거할 객체를 반환하는듯
			if (this.mapSessionNamesTokens.get(sessionName).remove(token) != null) {
				// 방이 비어있으면 방 자체를 지워준다
				if (this.mapSessionNamesTokens.get(sessionName).isEmpty()) {
					this.mapSessions.remove(sessionName);
				}
				return "redirect:/dashboard";

			} else {
				System.out.println("Problems in the app server: the TOKEN wasn't valid");
				return "redirect:/dashboard";
			}

		} else {
			System.out.println("Problems in the app server: the SESSION does not exist");
			return "redirect:/dashboard";
		}
	}
	
	private void checkUserLogged(HttpSession httpSession) throws Exception {
		if (httpSession == null || httpSession.getAttribute("loggedUser") == null) {
			throw new Exception("User not logged");
		}
	}
}