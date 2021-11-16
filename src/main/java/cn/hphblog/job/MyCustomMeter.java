package cn.hphblog.job;

import com.google.gson.Gson;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.dropwizard.metrics.DropwizardMeterWrapper;
import org.apache.flink.metrics.Meter;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;

import java.util.Properties;

public class MyCustomMeter {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);


        Properties props = new Properties();
        //指定kafka的Broker地址
        props.setProperty("bootstrap.servers", "hadoop102:9092,hadoop103:9092,hadoop104:9092");
        //设置组ID
        props.setProperty("group.id", "my_meter");
        props.setProperty("auto.offset.reset", "earliest");
        //kafka自动提交偏移量，
        props.setProperty("enable.auto.commit", "false");

//{"Id":295,"Name":"Nmae_295","Operation":"add","t":1636887111,"opt":0}
        FlinkKafkaConsumer<String> kafkaSouce = new FlinkKafkaConsumer<>("us_ac",  //指定Topic
                new SimpleStringSchema(),  //指定Schema，生产中一般使用Avro
                props);                     //Kafka配置

        //checkpoint开启
        environment.enableCheckpointing(5000);
        //重试策略开启
        environment.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(2, Time.seconds(2)));


        DataStreamSource<String> streamSource = environment.addSource(kafkaSouce);


        SingleOutputStreamOperator<Tuple2<String, Integer>> flatMap = streamSource.flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
            @Override
            public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) throws Exception {
                UserAction userAction = new Gson().fromJson(s, UserAction.class);
                collector.collect(Tuple2.of(userAction.getOperation(), userAction.getOpt()));
            }
        });

        //flink Meter Metrics
        flatMap.map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {
            private transient Meter meter;

            @Override
            public void open(Configuration parameters) throws Exception {
                com.codahale.metrics.Meter dropwizardMeter = new com.codahale.metrics.Meter();
                this.meter = getRuntimeContext()
                        .getMetricGroup()
                        .meter("myMeter", new DropwizardMeterWrapper(dropwizardMeter));

            }

            @Override
            public Tuple2<String, Integer> map(Tuple2<String, Integer> value) throws Exception {
                this.meter.markEvent();
                return value;
            }
        });

        KeyedStream<Tuple2<String, Integer>, String> tuple2StringKeyedStream = flatMap.keyBy(x -> x.f0);
        tuple2StringKeyedStream.timeWindow(org.apache.flink.streaming.api.windowing.time.Time.seconds(5)).
                reduce(new ReduceFunction<Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> reduce(Tuple2<String, Integer> t1, Tuple2<String, Integer> t2) throws
                            Exception {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                }).print();

        environment.execute("Flink Metrics Meter");

    }
}
