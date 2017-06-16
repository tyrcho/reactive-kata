package java8;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observers.DisposableObserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;

public class FileIO {

    public static void main(String[] args) throws IOException, InterruptedException {
        Observable<byte[]> observable = readFileAsync("java/pom.xml", 256);//.cache();
        Observable<String> strings = observable.map(String::new);

        // lambda version
        strings.subscribe(
                System.out::println,                        // onNext
                Throwable::printStackTrace,                 // onError
                () -> System.out.println("flow 1 complete")        // onComplete
        );

        // full version
        strings.subscribe(new DisposableObserver<String>() {
            @Override
            public void onNext(String s) {
                System.out.println("flow 2> " + s);
                dispose();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("flow 2 complete");
            }
        });

        Thread.sleep(2000); // <--- wait for the flows to finish
    }

    private static Observable<String> readFileAsyncBad() throws IOException {
        List<String> strings = Files.readAllLines(Paths.get("java/pom.xml"));
        return Observable.create(e -> {
            strings.forEach(e::onNext);
            e.onComplete();
        });
    }


    private static Observable<byte[]> readFileAsync(String filename, int capacity) {
        return Observable.create(emitter -> {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filename), READ);
            ByteBuffer buffer = ByteBuffer.allocate(capacity);
            emitter.setCancellable(channel::close);
            readChunk(channel, buffer, emitter, 0);
        });
    }

    private static void readChunk(AsynchronousFileChannel channel, ByteBuffer buffer, ObservableEmitter<byte[]> emitter, long position) {
        if (!emitter.isDisposed()) {
            System.out.println("start read from " + position);
            channel.read(buffer, position, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    System.out.println("read " + result +" bytes from " + position);
                    if (!emitter.isDisposed()) {
                        if (result > 0) {
                            byte[] fileData = new byte[result];
                            buffer.flip();
                            buffer.get(fileData);
                            buffer.clear();
                            emitter.onNext(fileData);
                            readChunk(channel, buffer, emitter, position + result);
                        } else {
                            emitter.onComplete();
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    emitter.onError(exc);
                }
            });
        }
    }
}
