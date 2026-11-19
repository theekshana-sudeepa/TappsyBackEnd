package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import socket.ProfileService;
import util.HibernateUtil;

@MultipartConfig
@WebServlet(name = "UserController", urlPatterns = {"/UserController"})
public class UserController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String countryCode = request.getParameter("countryCode");
        String contactNo = request.getParameter("contactNo");
        String pincode = request.getParameter("pincode");
        Part profileImage = request.getPart("profileImage");

        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        if (isBlank(firstName)) {
            responseObject.addProperty("message", "First name is required");
        } else if (isBlank(lastName)) {
            responseObject.addProperty("message", "Last name is required");
        } else if (isBlank(countryCode)) {
            responseObject.addProperty("message", "Country code is required");
        } else if (isBlank(contactNo)) {
            responseObject.addProperty("message", "Contact number is required");
        } else if (isBlank(pincode)) {
            responseObject.addProperty("message", "PIN is required");
        } else if (profileImage == null) {
            responseObject.addProperty("message", "Select a profile image");
        } else {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Integer createdUserId = null;
            User createdUser = null;
            try {
                Criteria c1 = s.createCriteria(User.class);
                c1.add(Restrictions.eq("countryCode", countryCode.trim()));
                c1.add(Restrictions.eq("contactNo", contactNo.trim()));

                User existingUser = (User) c1.uniqueResult();
                if (existingUser != null) {
                    responseObject.addProperty("message", "This contact number already exists!");
                } else {
                    createdUser = new User(
                            firstName.trim(),
                            lastName.trim(),
                            countryCode.trim(),
                            contactNo.trim(),
                            pincode.trim()
                    );
                    Date now = new Date();
                    createdUser.setCreatedAt(now);
                    createdUser.setUpdatedAt(now);

                    Transaction tr = s.beginTransaction();
                    createdUserId = (Integer) s.save(createdUser);
                    tr.commit();
                }
            } finally {
                s.close();
            }

            if (createdUserId != null && createdUser != null) {
                responseObject.add("user", gson.toJsonTree(createdUser));
                new ProfileService().saveProfileImage(createdUserId, request);
                responseObject.addProperty("status", true);
                responseObject.addProperty("userId", createdUserId);
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
