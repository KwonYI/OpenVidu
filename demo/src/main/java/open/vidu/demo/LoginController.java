package open.vidu.demo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.openvidu.java.client.OpenViduRole;

@Controller
public class LoginController {

	public class MyUser {

		String name;
		String pass;
		OpenViduRole role;

		public MyUser(String name, String pass, OpenViduRole role) {
			this.name = name;
			this.pass = pass;
			this.role = role;
		}
	}

	public static Map<String, MyUser> users = new ConcurrentHashMap<>();

	public LoginController() {
		users.put("p", new MyUser("publisher", "p", OpenViduRole.PUBLISHER));
		users.put("m", new MyUser("moderator", "m", OpenViduRole.MODERATOR));
		users.put("s", new MyUser("subscriber", "s", OpenViduRole.SUBSCRIBER));
	}

	@RequestMapping(value = "/")
	public String logout(HttpSession httpSession) {
		if (checkUserLogged(httpSession)) {
			return "redirect:/dashboard";
		} else {
			httpSession.invalidate();
			return "index";
		}
	}

	@RequestMapping(value = "/dashboard", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(@RequestParam(name = "user", required = false) String user,
			@RequestParam(name = "pass", required = false) String pass, Model model, HttpSession httpSession) {
		String userName = (String) httpSession.getAttribute("loggedUser");
		if (userName != null) {
			model.addAttribute("username", userName);
			return "dashboard";
		}
		
		if (login(user, pass)) {
			httpSession.setAttribute("loggedUser", user);
			model.addAttribute("username", user);
			return "dashboard";

		} else {
			httpSession.invalidate();
			return "redirect:/";
		}
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public String logout(Model model, HttpSession httpSession) {
		httpSession.invalidate();
		return "redirect:/";
	}

	private boolean login(String user, String pass) {
		return (user != null && pass != null && users.containsKey(user) && users.get(user).pass.equals(pass));
	}

	private boolean checkUserLogged(HttpSession httpSession) {
		return !(httpSession == null || httpSession.getAttribute("loggedUser") == null);
	}
}