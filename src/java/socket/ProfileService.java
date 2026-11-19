package socket;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import util.AppConfig;

public class ProfileService {

    public boolean saveProfileImage(int userId, HttpServletRequest request) throws IOException, ServletException {
        Part profileImage = request.getPart("profileImage");
        String appPath = request.getServletContext().getRealPath("");

        String newPath = appPath.replace("build" + File.separator + "web", "web" + File.separator + "profile-images");

        File profileFolder = new File(newPath, String.valueOf(userId));
        if (!profileFolder.exists()) {
            profileFolder.mkdirs();
        }

        File file1 = new File(profileFolder, "profile1.png");
        Files.copy(profileImage.getInputStream(), file1.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return true;
    }

    public static String getProfileUrl(int userId) {
        HttpURLConnection conn = null;
        try {
            String assetPath = AppConfig.getContextPath() + "/profile-images/" + userId + "/profile1.png";
            URL url = new URI(AppConfig.getLocalBaseUrl() + assetPath).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return AppConfig.getPublicBaseUrl() + assetPath;
            }
            return "";
        } catch (IOException | URISyntaxException e) {
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
