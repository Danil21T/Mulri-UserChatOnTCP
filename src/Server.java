import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class Server {
    private static ServerSocket serverSocket;
    private static UsersListServer model; //объект класса модели
    private static volatile boolean isServerStart = false; //флаг отражающий состояние сервера запущен/остановлен

    //метод, запускающий сервер
    public static void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isServerStart = true;
            System.out.println("Server is job");
            new ServerClose().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //метод останавливающий сервер
    protected static void stopServer() {
        try {
            //если серверныйСокет не имеет ссылки или не запущен
            if (serverSocket != null && !serverSocket.isClosed()) {
                for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
                    user.getValue().close();
                }
                serverSocket.close();
                model.getAllUsersMultiChat().clear();
                System.out.println("Server stop");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //метод, в котором в бесконечном цикле сервер принимает новое сокетное подключение от клиента
    protected void acceptServer() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
            } catch (Exception e) {
                System.out.println("Connection with server is lost");
                break;
            }
        }
    }

    //метод, рассылающий заданное сообщение всем клиентам из мапы
    protected void sendMessageAllUsers(Message message) {
        for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
            try {
                user.getValue().send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //точка входа для приложения сервера
    public static void main(String[] args) {
        Server server = new Server();
        model = new UsersListServer();
        System.out.println("Enter the port");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        startServer(port);
        //цикл снизу ждет true от флага isServerStart (при старте сервера в методе startServer устанавливается в true)
        //после чего запускается бесконечный цикл принятия подключения от клиента в  методе acceptServer
        //до тех пор пока сервер не остановится, либо не возникнет исключение
        while (true) {
            if (isServerStart) {
                server.acceptServer();
                isServerStart = false;
            }
        }
    }

    //класс-поток, который запускается при принятии сервером нового сокетного соединения с клиентом, в конструктор
    //передается объект класса Socket
    private static class ServerClose extends Thread{
        @Override
        public void run() {
            while(true){
                Scanner scanner = new Scanner(System.in);
                if(scanner.nextLine().contains("@quit")){
                    stopServer();
                    break;
                }

            }
        }
    }

    private class ServerThread extends Thread {
        private Socket socket;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        //метод который реализует запрос сервера у клиента имени и добавлении имени в мапу
        private String requestAndAddingUser(Connection connection) {
            while (true) {
                try {
                    //посылаем клиенту сообщение-запрос имени
                    connection.send(new Message(MessageType.REQUEST_NAME_USER));
                    Message responseMessage = connection.receive();
                    String userName = responseMessage.getTextMessage();
                    //получили ответ с именем и проверяем не занято ли это имя другим клиентом
                    if (responseMessage.getTypeMessage() == MessageType.USER_NAME && userName != null && !userName.isEmpty() && !model.getAllUsersMultiChat().containsKey(userName)) {
                        //добавляем имя в мапу
                        model.addUser(userName, connection);
                        Set<String> listUsers = new HashSet<>();
                        for (Map.Entry<String, Connection> users : model.getAllUsersMultiChat().entrySet()) {
                            listUsers.add(users.getKey());
                        }
                        //отправляем клиенту множетство имен всех уже подключившихся пользователей
                        connection.send(new Message(MessageType.NAME_ACCEPTED, listUsers));
                        //отправляем всем клиентам сообщение о новом пользователе
                        sendMessageAllUsers(new Message(MessageType.USER_ADDED, userName));
                        return userName;
                    }
                    //если такое имя уже занято отправляем сообщение клиенту, что имя используется
                    else connection.send(new Message(MessageType.NAME_USED));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //метод, реализующий обмен сообщениями между пользователями
        private void messagingBetweenUsers(Connection connection, String userName) {
            while (true) {
                try {
                    Message message = connection.receive();
                    //приняли сообщение от клиента, если тип сообщения TEXT_MESSAGE то пересылаем его всем пользователям
                    if (message.getTypeMessage() == MessageType.TEXT_MESSAGE) {
                        String textMessage = String.format("%s: %s\n", userName, message.getTextMessage());
                        sendMessageAllUsers(new Message(MessageType.TEXT_MESSAGE, textMessage));
                    }
                    //если тип сообщения DISABLE_USER, то рассылаем всем пользователям, что данный пользователь покинул чат,
                    //удаляем его из мапы, закрываем его connection
                    if (message.getTypeMessage() == MessageType.DISABLE_USER) {
                        sendMessageAllUsers(new Message(MessageType.REMOVED_USER, userName));
                        model.removeUser(userName);
                        connection.close();
                        System.out.println("Users is disconnect " + socket.getRemoteSocketAddress());
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        @Override
        public void run() {
            System.out.println("Users connect " + socket.getRemoteSocketAddress());
            try {
                //получаем connection при помощи принятого сокета от клиента и запрашиваем имя, регистрируем, запускаем
                //цикл обмена сообщениями между пользователями
                Connection connection = new Connection(socket);
                String nameUser = requestAndAddingUser(connection);
                messagingBetweenUsers(connection, nameUser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}