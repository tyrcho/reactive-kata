package java8;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;

public class FileIO {


    private static Disposable subscription;

    public static void main(String[] args) throws IOException, InterruptedException {
        Observable<byte[]> flowable = readFileAsync("java/pom.xml", 20);//.cache();
        Observable<String> strings = flowable.map(String::new);

//        subscription = strings.subscribe(
//                s -> {
//                    System.out.println("> " + s);
//                    subscription.dispose();
//                },
//                e -> {
//                    e.printStackTrace();
//                    //System.exit(1);
//                });
//        //,                        () -> System.exit(0));

        strings.subscribe(new Observer<String>() {
            public Disposable subscription;

            @Override
            public void onSubscribe(Disposable d) {
                this.subscription = d;
            }

            @Override
            public void onNext(String s) {
                    System.out.println("> " + s);
                    //subscription.dispose();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("complete");
            }
        });
        Thread.sleep(500000);
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
            readChunk(channel, buffer, emitter, 0);
        });

    }

    private static void readChunk(AsynchronousFileChannel channel, ByteBuffer buffer, ObservableEmitter<byte[]> emitter, long position) {
        channel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (emitter.isDisposed()) close(channel);
                else if (result > 0) {
                    byte[] fileData = new byte[result];
                    System.out.println("read from " + position);
                    System.arraycopy(attachment.array(), 0, fileData, 0, result);
                    buffer.clear();
                    emitter.onNext(fileData);
                    readChunk(channel, buffer, emitter, position + result);
                } else {
                    emitter.onComplete();
                    close(channel);
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                emitter.onError(exc);
                close(channel);
            }
        });
    }

    private static void close(AsynchronousFileChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
