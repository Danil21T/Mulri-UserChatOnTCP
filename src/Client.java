import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Connection connection;
    private static UsersListClient model;
    private static volatile boolean isConnect = false; //флаг отобаржающий состояние подключения клиента серверу

    public boolean isConnect() {
        return isConnect;
    }

    public static void setConnect(boolean connect) {
        isConnect = connect;
    }

    //точка входа в клиентское приложение
    public static void main(String[] args) {
        Client client = new Client();
        model = new UsersListClient();
        connectToServer();
        while (true) {
            if (client.isConnect()) {
                client.nameUserRegistration();
                client.setConnect(false);
                Scanner scanner = new Scanner(System.in);
                sendMessageOnServer(scanner.nextLine());
            }
        }
    }

    //метод подключения клиента  серверу
    public static void connectToServer() {
        //если клиент не подключен  сервере то..
        if (!isConnect) {
            while (true) {
                try {
                    DatagramSocket datagramSocket = new DatagramSocket();
                    InetAddress inetAddress = InetAddress.getByName("localhost");
                    Scanner read = new Scanner(System.in);
                    System.out.println("Enter the number of port: ");
                    int port = read.nextInt();
                    Socket socket = new Socket(inetAddress, port);
                    connection = new Connection(socket);
                    isConnect = true;
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    //метод, реализующий регистрацию имени пользователя со стороны клиентского приложения
    protected void nameUserRegistration() {
        while (true) {
            try {
                Message message = connection.receive();
                //приняли от сервера сообщение, если это запрос имени, то вызываем окна ввода имени, отправляем на сервер имя
                if (message.getTypeMessage() == MessageType.REQUEST_NAME_USER) {
                    System.out.println("Enter the name");
                    Scanner scanner = new Scanner(System.in);
                    String nameUser = scanner.nextLine();
                    connection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //если сообщение - имя уже используется, выводим соответствующее оуно с ошибой, повторяем ввод имени
                if (message.getTypeMessage() == MessageType.NAME_USED) {
                    System.out.println("Name is occupied");
                    Scanner scanner = new Scanner(System.in);
                    String nameUser = scanner.nextLine();
                    connection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //если имя принято, получаем множество всех подключившихся пользователей, выходим из цикла
                if (message.getTypeMessage() == MessageType.NAME_ACCEPTED) {
                    System.out.println("Name is accepted");
                    model.setUsers(message.getListUsers());
                    break;
                }
                new MessageFromServer().start();
                new MessageClient().start();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    connection.close();
                    isConnect = false;
                    break;
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }

        }
    }

    //метод отправки сообщения предназначенного для других пользователей на сервер
    public static void sendMessageOnServer(String text) {
        try {
            connection.send(new Message(MessageType.TEXT_MESSAGE, text));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //метод принимающий с сервера сообщение от других клиентов
    private static class MessageFromServer extends Thread {
        @Override
        public void run() {
            while (isConnect) {
                try {
                    Message message = connection.receive();
                    //если тип TEXT_MESSAGE, то добавляем текст сообщения в окно переписки
                    if (message.getTypeMessage() == MessageType.TEXT_MESSAGE) {
                        System.out.println(message.getTextMessage());
                    }
                    //если сообщение с типо USER_ADDED добавляем сообщение в окно переписки о новом пользователе
                    if (message.getTypeMessage() == MessageType.USER_ADDED) {
                        model.addUser(message.getTextMessage());
                        System.out.println(String.format("User " + message.getTextMessage()
                                + " connect."));
                    }
                    //аналогично для отключения других пользователей
                    if (message.getTypeMessage() == MessageType.REMOVED_USER) {
                        model.removeUser(message.getTextMessage());
                        System.out.println(String.format("User " + message.getTextMessage() + " disconnect"));
                    }
                } catch (Exception e) {
                    setConnect(false);
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    //метод реализующий отключение нашего клиента от чата
    protected static void disableClient() {
        try {
            if (isConnect) {
                connection.send(new Message(MessageType.DISABLE_USER));
                model.getUsers().clear();
                isConnect = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MessageClient extends Thread {
        @Override
        public void run() {
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String mes = scanner.nextLine();
                if (mes.contains("@quit")) {
                    disableClient();
                    break;
                }
            }
        }
    }
}