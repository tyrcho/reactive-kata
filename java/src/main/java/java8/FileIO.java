package java8;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;

public class FileIO {

    public static void main(String[] args) throws IOException, InterruptedException {
        Observable<byte[]> flowable = readFileAsync2("java/pom.xml");
        flowable
                .map(String::new)
                .subscribe(s -> System.out.println("aa" + s));
        Thread.sleep(500000);
    }

    private static Observable<String> readFileAsync() throws IOException {
        List<String> strings = Files.readAllLines(Paths.get("java/pom.xml"));
        return Observable.create(e -> {
            strings.forEach(e::onNext);
            e.onComplete();
        });
    }


    private static Observable<byte[]> readFileAsync2(String filename) throws IOException {
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filename), READ);
        ByteBuffer buffer = ByteBuffer.allocate(256);

        return Observable.create(obs -> {
            long position = 0L;
            readChunk(channel, buffer, obs, position);
        });

    }

    private static void readChunk(AsynchronousFileChannel channel, ByteBuffer buffer, ObservableEmitter<byte[]> obs, long position) {
        channel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                try {
                    if (result > 0) {
                        byte[] fileData = new byte[result];
                        System.arraycopy(attachment.array(), 0, fileData, 0, result);
                        buffer.clear();
                        obs.onNext(fileData);
                        //if (result == 256) {
                            readChunk(channel, buffer, obs, position + result);
                        //} else
                        //    obs.onComplete();
                    } else {
                        obs.onComplete();
                    }
                } catch (Exception e) {
                    obs.onError(e);
                } finally {
//                    close(channel);
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                obs.onError(exc);
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
