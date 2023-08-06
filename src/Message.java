import java.io.Serializable;
import java.util.Set;

enum MessageType {
    REQUEST_NAME_USER,
    TEXT_MESSAGE,
    NAME_ACCEPTED,
    USER_NAME,
    NAME_USED,
    USER_ADDED,
    DISABLE_USER,
    REMOVED_USER;
}

public class Message implements Serializable {
    private MessageType typeMessage; //тип сообщения
    private String textMessage; //текст сообщения
    private Set<String> listUsers; //множество имен уже подключившихся пользователей

    public Message(MessageType typeMessage, String textMessage) {
        this.textMessage = textMessage;
        this.typeMessage = typeMessage;
        this.listUsers = null;
    }

    public Message(MessageType typeMessage, Set<String> listUsers) {
        this.typeMessage = typeMessage;
        this.textMessage = null;
        this.listUsers = listUsers;
    }

    public Message(MessageType typeMessage) {
        this.typeMessage = typeMessage;
        this.textMessage = null;
        this.listUsers = null;
    }

    public MessageType getTypeMessage() {
        return typeMessage;
    }

    public Set<String> getListUsers() {
        return listUsers;
    }

    public String getTextMessage() {
        return textMessage;
    }

}