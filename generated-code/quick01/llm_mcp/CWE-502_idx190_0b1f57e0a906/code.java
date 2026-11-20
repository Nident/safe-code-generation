import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public static void listen(final Consumer<String[]> consumer) throws Exception {
    final ServerSocket server = new ServerSocket(PORT);
    try {
        Thread listener = new Thread(() -> {
            try (ServerSocket listeningSocket = server) {
                while (true) {
                    Socket accepted = listeningSocket.accept();
                    try (Socket socket = accepted;
                         ObjectInputStream input = new ObjectInputStream(socket.getInputStream()) {
                             @Override
                             protected Class<?> resolveClass(ObjectStreamClass descriptor)
                                     throws IOException, ClassNotFoundException {
                                 String name = descriptor.getName();
                                 if (String[].class.getName().equals(name)) {
                                     return String[].class;
                                 }
                                 if (String.class.getName().equals(name)) {
                                     return String.class;
                                 }
                                 throw new InvalidClassException(name, "Unexpected serialized class");
                             }

                             @Override
                             protected Class<?> resolveProxyClass(String[] interfaces)
                                     throws IOException, ClassNotFoundException {
                                 throw new InvalidClassException("Serialized proxy classes are unsupported");
                             }
                         }) {
                        Object message = input.readObject();
                        if (!(message instanceof String[])) {
                            throw new StreamCorruptedException("Expected a serialized String array");
                        }
                        consumer.accept((String[]) message);
                    } catch (IOException | ClassNotFoundException failure) {
                        ExceptionUtil.printStackTrace(failure);
                    }
                }
            } catch (IOException failure) {
                ExceptionUtil.printStackTrace(failure);
            }
        }, "string-array-listener");
        listener.setDaemon(true);
        listener.start();
    } catch (RuntimeException | Error startupFailure) {
        try {
            server.close();
        } catch (IOException closeFailure) {
            startupFailure.addSuppressed(closeFailure);
        }
        throw startupFailure;
    }
}
