package app;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class LineBreakPipe {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream("access_data");

        KStream<String, String> words = source.flatMapValues(value -> Arrays.asList(value.split("\\W+")));
        KStream<String, String> filter = words.filter((key, message) -> {
            System.out.println(key);
            System.out.println(message);
            return message.length() > 4;
        });

        filter.to("access_data_out");

        final Topology topology = builder.build();
        System.out.println(topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, properties);
        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested...");
            streams.close();
            latch.countDown();
            System.out.println("Finished");
        }));


        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
}
