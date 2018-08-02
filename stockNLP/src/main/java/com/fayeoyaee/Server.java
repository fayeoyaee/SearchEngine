package com.fayeoyaee;

import java.io.*;
import java.util.*;
import java.net.*;

public class Server {
  private static final String OUTPUT_SUCCESS_HEADERS = "HTTP/1.1 200 OK\r\n" +
    "Content-Type: application/json;charset=UTF-8\r\n" + 
    "Access-Control-Allow-Origin: *\r\n" +
    "Content-Length: ";
  private static final String OUTPUT_FAILURE_HEADERS = "HTTP/1.1 500 Error\r\n" +
    "Content-Type: application/json\r\n" + 
    "Content-Length: ";
  private static final String OUTPUT_END_OF_HEADERS = "\r\n\r\n";

  public static Map<String, String> requestParser(String request) {
    // "GET /popWords?date=07-29?n=10 HTTP/1.1"
    // "GET /queryStock?stockName=华谊兄弟?dates=07-29,07-28 HTTP/1.1"
    // "GET /query?queryString=公司?winSize=2 HTTP/1.1"
    try {
      request = URLDecoder.decode(request, "utf-8");
      System.out.println("[Server.main]: parsing request: " + request);
      request = request.substring(request.indexOf("/"), request.indexOf("HTTP")-1); // ignore header for now; assume only parse GET reqeust
      String[] parts = request.split("\\?"); // should get "/querystock" "?date=07-29" "?n=10"
      Map<String, String> ret = new HashMap<>();
      ret.put("EndPoint", parts[0].substring(1));
      for (int i = 1; i < parts.length; ++i) {
        String[] subParts = parts[i].split("=");
        ret.put(subParts[0], subParts[1]);
      }
      return ret;
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
	}

  public static void main(String[] args) {
    try {
      Query.init();
      Query q = new Query();

      ServerSocket server = new ServerSocket(9090); 
      // Server always listen to port
      while (true) { 
        Socket clientSocket = server.accept(); 
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  

        String output = null;
        String line = reader.readLine(); 
        boolean success = true;
        while (line != null && !line.isEmpty()) { 
          Map<String, String> parsedReq = Server.requestParser(line);
          if (parsedReq.get("EndPoint").equals("popWords")) {
            if (parsedReq.containsKey("date") && parsedReq.containsKey("n")) {
              System.out.println("[Server.main]: sending request to Query.popWords");
              output = q.popWords(parsedReq.get("date"), Integer.valueOf(parsedReq.get("n")));
            } else {
              output = "Missing data or number of retrieval";
              success = false;
            }
          } else if (parsedReq.get("EndPoint").equals("queryStock")) {
            if (parsedReq.containsKey("stockName") && parsedReq.containsKey("dates")) {
              System.out.println("[Server.main]: sending request to Query.queryStock");
              output = q.queryStock(parsedReq.get("stockName"), Arrays.asList(parsedReq.get("dates").split(",")));
            } else {
              output = "Missing stock name or dates";
              success = false;
            }
          } else if (parsedReq.get("EndPoint").equals("query")) {
            if (parsedReq.containsKey("queryString") && parsedReq.containsKey("winSize")) {
              output = q.query(parsedReq.get("queryString"), Integer.valueOf(parsedReq.get("winSize")));
            } else {
              output = "Missing query string or winSize";
              success = false;
            }
          }

          break; // assume we only care about the first line 
          // System.out.println(line);
          // line=reader.readLine();
        } 

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSocket.getOutputStream()), "UTF-8"));
        if (output != null) {
          // System.out.println("[Server.main]: writing output: " + output);
          writer.write((success ? OUTPUT_SUCCESS_HEADERS : OUTPUT_FAILURE_HEADERS) + output.getBytes().length + OUTPUT_END_OF_HEADERS + output);
        }
        writer.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Query.close();
    }
  }
}
