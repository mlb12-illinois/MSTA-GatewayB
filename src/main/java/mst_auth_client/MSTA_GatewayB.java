package mst_auth_client;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import mst_auth_library.MSTAException;
import mst_auth_library.MST_Auth_BaseClientWrapper;
import mst_auth_library.MST_Auth_BaseServlet;
import mst_auth_library.MST_Auth_ClientWrapper;
import mst_auth_library.MST_Auth_Servlet;
import mst_auth_library.MST_Auth_Microservice;

public class MSTA_GatewayB extends MST_Auth_Microservice {
	public Semaphore mysemaphore;
	String resp;		// this does not get reset each call
	public MSTA_GatewayB() {
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response, String trustedbody) throws IOException, MSTAException {
		mysemaphore = new Semaphore(1);	// must be > 1
		//System.out.println("Base");
		String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		//System.out.println(input);
		resp = new String();
		JSONObject newobj = new JSONObject(input);		
		String communication = newobj.getString("communication");
		int iterations = newobj.getInt("iterations");
		String payload = newobj.getString("payload");
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			if (communication.equals("Synchronous")) {
				msta_library.SetMicroservice("http://localhost:8080/MSTA-BusinessServiceB/MSTABusinessB.html");
				msta_library.SetMethodWithBodyString("GET", payload);
				msta_library.SetHeader("Content-Type", "text; utf-8");
				HttpResponse myresponse = msta_library.SendRequest();
			}
			else if (communication.equals("Asynchronous")) {
				msta_library.SetMicroservice("http://localhost:8080/MSTA-BusinessServiceB/MSTABusinessB.html");
				msta_library.SetMethodWithBodyString("GET", payload);
				msta_library.SetHeader("Content-Type", "text; utf-8");
				msta_library.SendRequestA();
			}
		}
		if (communication.equals("Asynchronous")) msta_library.WaitA();

		long endTime = System.currentTimeMillis();
		String strout = new String("Server,Base,Communication," + communication + ",Iterations," + iterations + ",Size," + payload.length() + ", Start," + startTime + ",End," + endTime + ",Total," + (endTime - startTime) );
		
		//System.out.println("Leaving");
		response.getWriter().append(strout);
	}
	// OY This has to be thread safe
	public void callbackResponse(HttpResponse<String> parmmstresponse) {
		//System.out.println("callbackResponse");
		try {
			mysemaphore.acquire();
			if (parmmstresponse != null)
				resp = resp + parmmstresponse.body().toString() + ": ";
			mysemaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//System.out.println("callbackResponse2");
	}
}
