package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import socket.ProfileService;

@MultipartConfig
@WebServlet(name = "ProfileController", urlPatterns = {"/ProfileController"})
public class ProfileController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part profileImage = request.getPart("profileImage");
        String userIdParam = request.getParameter("userId");

        int userId = 0;
        if (userIdParam != null && !userIdParam.trim().isEmpty()) {
            try {
                userId = Integer.parseInt(userIdParam.trim());
            } catch (NumberFormatException ignored) {
                userId = 0;
            }
        }

        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", Boolean.FALSE);

        if (userId <= 0) {
            responseObject.addProperty("message", "OOPS! User not found");
        } else if (profileImage == null) {
            responseObject.addProperty("message", "Upload your profile image");
        } else {
            boolean isSuccess = new ProfileService().saveProfileImage(userId, request);
            if (isSuccess) {
                responseObject.addProperty("status", Boolean.TRUE);
                responseObject.addProperty("message", "Profile image updated successfully");
            } else {
                responseObject.addProperty("message", "Profile image uploading failed");
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
