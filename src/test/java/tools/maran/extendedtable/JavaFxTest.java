package tools.maran.extendedtable;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

/// Base class for all tests which need a running JavaFX toolkit.
///
/// The toolkit is started once per JVM and runs headless.
///
/// @author Marius Hanl
public abstract class JavaFxTest {

    private static final long TIMEOUT_SECONDS = 2;

    private static boolean toolkitStarted;

    @AfterEach
    protected void closeWindows() {
        runOnFxThread(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    /// Runs the given [Supplier] on the JavaFX application thread and returns its value.
    ///
    /// @param supplier
    ///         the [Supplier] to run
    /// @param <T>
    ///         the type of the value
    /// @return the value supplied on the JavaFX application thread
    protected static <T> T getOnFxThread(Supplier<T> supplier) {
        AtomicReference<T> valueRef = new AtomicReference<>();
        runOnFxThread(() -> valueRef.set(supplier.get()));
        return valueRef.get();
    }

    /// Runs the given [Runnable] on the JavaFX application thread and waits until it is finished.
    /// Any [Throwable] thrown inside the [Runnable], e.g. a failed assertion, is rethrown on the calling thread.
    ///
    /// @param runnable
    ///         the [Runnable] to run
    protected static void runOnFxThread(Runnable runnable) {
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                throwableRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        await(latch);

        Throwable throwable = throwableRef.get();
        switch (throwable) {
            case null -> {
                // Everything went fine.
            }
            case RuntimeException runtimeException -> throw runtimeException;
            case Error error -> throw error;
            default -> throw new IllegalStateException(throwable);
        }
    }

    /// Shows the given root inside a new [Stage] with the given size and lays it out.
    /// All windows are closed after each test.
    ///
    /// @param root
    ///         the root of the [Scene]
    /// @param width
    ///         the width of the [Scene]
    /// @param height
    ///         the height of the [Scene]
    /// @return the shown [Stage]
    protected static Stage showInStage(Parent root, double width, double height) {
        Stage stage = new Stage();
        stage.setScene(new Scene(root, width, height));
        stage.show();
        root.layout();
        return stage;
    }

    @BeforeAll
    static void initToolkit() {
        if (toolkitStarted) {
            return;
        }
        toolkitStarted = true;

        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            // Otherwise, the toolkit exits as soon as a test closes the last window.
            Platform.setImplicitExit(false);
            Application.setUserAgentStylesheet(null);
            latch.countDown();
        });

        await(latch);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "The JavaFX application thread did not finish within " + TIMEOUT_SECONDS + " seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the JavaFX application thread.", e);
        }
    }

}
