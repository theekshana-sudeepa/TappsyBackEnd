package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import util.HibernateUtil;

@WebServlet(name = "LoginController", urlPatterns = {"/LoginController"})
@MultipartConfig
public class LoginController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String contactNo = null;
        String pincode = null;

        String contentType = request.getContentType();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            try {
                Part contactNoPart = request.getPart("contactNo");
                Part pincodePart = request.getPart("pincode");

                if (contactNoPart != null) {
                    contactNo = getPartValue(contactNoPart);
                }

                if (pincodePart != null) {
                    pincode = getPartValue(pincodePart);
                }
            } catch (IOException | ServletException e) {
                System.err.println("Error parsing multipart login request: " + e.getMessage());
            }
        } else {
            contactNo = request.getParameter("contactNo");
            pincode = request.getParameter("pincode");

            if (contactNo == null || pincode == null) {
                Map<String, String> params = parseRequestBody(request);
                if (contactNo == null) {
                    contactNo = params.get("contactNo");
                }
                if (pincode == null) {
                    pincode = params.get("pincode");
                }
            }
        }

        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        if (contactNo == null || contactNo.trim().isEmpty()) {
            responseObject.addProperty("message", "Contact number is required");
        } else if (pincode == null || pincode.trim().isEmpty()) {
            responseObject.addProperty("message", "PIN is required");
        } else if (!pincode.matches("\\d{4}")) {
            responseObject.addProperty("message", "PIN must be exactly 4 digits");
        } else {
            Session s = HibernateUtil.getSessionFactory().openSession();
            try {
                Criteria c1 = s.createCriteria(User.class);
                c1.add(Restrictions.eq("contactNo", contactNo.trim()));
                c1.add(Restrictions.eq("pincode", pincode.trim()));

                User user = (User) c1.uniqueResult();

                if (user != null) {
                    responseObject.addProperty("status", true);
                    responseObject.addProperty("message", "Login successful");
                    responseObject.addProperty("id", user.getId());
                    responseObject.add("user", gson.toJsonTree(user));
                } else {
                    responseObject.addProperty("message", "Invalid contact number or PIN");
                }
            } finally {
                s.close();
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private String getPartValue(Part part) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(part.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder value = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                value.append(line);
            }
            return value.toString().trim();
        }
    }

    private Map<String, String> parseRequestBody(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        try {
            BufferedReader reader = request.getReader();
            StringBuilder body = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            String bodyString = body.toString();

            if (!bodyString.isEmpty()) {
                String[] pairs = bodyString.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        params.put(
                                URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()),
                                URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name())
                        );
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error parsing request body: " + e.getMessage());
        }

        return params;
    }
}
