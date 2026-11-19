package socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import entity.Chat;
import entity.Status;
import entity.User;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import util.HibernateUtil;

@ServerEndpoint(value = "/chat")
public class ChatEndPoint {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private int userId;

    @OnOpen
    public void onOpen(Session session) {
        String query = session.getQueryString();
        if (query != null && query.startsWith("userId=")) {
            userId = Integer.parseInt(query.substring("userId=".length()));
            ChatService.register(userId, session);
            UserService.updateLogInStatus(userId);
            UserService.updateFriendChatStatus(userId);
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (userId > 0) {
            ChatService.unregister(userId);
            UserService.updateLogOutStatus(userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (userId > 0) {
            UserService.updateLogOutStatus(userId);
        }
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> map = GSON.fromJson(message, MAP_TYPE);
            if (map == null || map.get("type") == null) {
                return;
            }
            String type = String.valueOf(map.get("type"));
            switch (type) {
                case "PING": {
                    JsonObject responseObject = new JsonObject();
                    responseObject.addProperty("type", "PONG");
                    ChatService.sendToUser(userId, responseObject);
                    break;
                }
                case "send_chat": {
                    int fromId = toInt(map.get("fromId"));
                    int toId = toInt(map.get("toId"));
                    String chatText = String.valueOf(map.get("message"));
                    org.hibernate.Session s = HibernateUtil.getSessionFactory().openSession();
                    User fromUser;
                    User toUser;
                    try {
                        fromUser = (User) s.get(User.class, fromId);
                        toUser = (User) s.get(User.class, toId);
                    } finally {
                        s.close();
                    }

                    if (fromUser != null && toUser != null) {
                        Chat chat = new Chat(fromUser, chatText, toUser, "", Status.SENT);
                        Date now = new Date();
                        chat.setCreatedAt(now);
                        chat.setUpdatedAt(now);
                        ChatService.deliverChat(chat);
                    }
                    break;
                }
                case "get_chat_list": {
                    ChatService.sendToUser(userId,
                            ChatService.friendListEnvelope(ChatService.getFriendChatsForUser(userId)));
                    break;
                }
                case "get_single_chat": {
                    int friendId = toInt(map.get("friendId"));
                    List<Chat> chats = ChatService.getChatHistory(userId, friendId);
                    Map<String, Object> envelope = ChatService.singleChatEnvelope(chats);
                    ChatService.sendToUser(userId, envelope);
                    ChatService.sendToUser(userId,
                            ChatService.friendListEnvelope(ChatService.getFriendChatsForUser(userId)));
                    break;
                }
                case "send_message": {
                    int friendId = toInt(map.get("toUserId"));
                    String chat = String.valueOf(map.get("message"));
                    ChatService.saveNewChat(userId, friendId, chat);
                    break;
                }
                case "get_friend_data": {
                    int friendId = toInt(map.get("friendId"));
                    Map<String, Object> envelope = UserService.getFriendData(friendId);
                    ChatService.sendToUser(userId, envelope);
                    break;
                }
                case "get_all_users": {
                    Map<String, Object> envelope = UserService.getAllUsers(userId);
                    ChatService.sendToUser(userId, envelope);
                    break;
                }
                case "save_new_contact": {
                    Map<?, ?> userObject = (Map<?, ?>) map.get("user");
                    if (userObject != null) {
                        User user = new User(
                                String.valueOf(userObject.get("firstName")),
                                String.valueOf(userObject.get("lastName")),
                                String.valueOf(userObject.get("countryCode")),
                                String.valueOf(userObject.get("contactNo")),
                                String.valueOf(userObject.get("pincode")));
                        Map<String, Object> envelope = UserService.saveNewContact(userId, user);
                        ChatService.sendToUser(userId, envelope);
                        Map<String, Object> allUsers = UserService.getAllUsers(userId);
                        ChatService.sendToUser(userId, allUsers);
                    }
                    break;
                }
                case "set_user_profile": {
                    Map<String, Object> envelope = UserService.getMyProfileData(userId);
                    ChatService.sendToUser(userId, envelope);
                    break;
                }
                default: {
                    System.out.println("Ignored unknown client type: " + type);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
