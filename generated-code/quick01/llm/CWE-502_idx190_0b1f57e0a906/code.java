import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public static void listen(final Consumer<String[]> consumer) throws Exception {
    final ServerSocket server = new ServerSocket(PORT);
    final Thread listener = new Thread(() -> {
        try (ServerSocket listeningSocket = server) {
            while (true) {
                final Socket accepted = listeningSocket.accept();
                try (Socket socket = accepted;
                     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                    final Object message = input.readObject();
                    if (!(message instanceof String[])) {
                        throw new IOException("Expected a serialized String[]");
                    }
                    consumer.accept((String[]) message);
                } catch (IOException | ClassNotFoundException exception) {
                    ExceptionUtil.printStackTrace(exception);
                }
            }
        } catch (IOException exception) {
            ExceptionUtil.printStackTrace(exception);
        }
    });
    try {
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
