package software.amazon.samples;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.amazon.awscdk.services.elasticsearch.CfnDomain;
import software.amazon.awscdk.services.elasticsearch.CfnDomainProps;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.msk.CfnClusterProps;

import java.util.stream.Collectors;

public class EventSourcingInfraStack extends Stack {
    public EventSourcingInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public EventSourcingInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // Vpc setup
        Vpc vpc = new Vpc(this, "EventSourcingVPC", VpcProps.builder().build());

        // Kafka
        CfnCluster kafkaCluster = eventSourcingKafka(vpc);

        // Elasticsearch
        CfnDomain elasticsearchCluster = eventSourcingElasticsearch(vpc);

        // S3 webserver


        // 3 x ECS Fargate

        // Output
//        CfnOutput output = new CfnOutput(this, "kafkaUrl", CfnOutputProps.builder()
//            .value(kafkaCluster.getBrokerNodeGroupInfo().toString())
//            .build());
    }

    private CfnCluster eventSourcingKafka(Vpc vpc) {
        return  new CfnCluster(this, "EventSourcingKafka",
            CfnClusterProps.builder()
                .clusterName("EventSourcingKafkaCluster")
                .kafkaVersion("2.3.1")
                .numberOfBrokerNodes(3)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                    .instanceType("kafka.m5.large")
                    .storageInfo(CfnCluster.StorageInfoProperty.builder()
                        .ebsStorageInfo(CfnCluster.EBSStorageInfoProperty.builder()
                            .volumeSize(40)
                            .build())
                        .build())
                    .clientSubnets(vpc.getPrivateSubnets().stream().map(s -> s.getSubnetId()).collect(Collectors.toList()))
                    .build())
                .encryptionInfo(CfnCluster.EncryptionInfoProperty.builder()
                    .encryptionInTransit(CfnCluster.EncryptionInTransitProperty.builder()
                        .clientBroker("PLAINTEXT")
                        .build())
                    .build())
                .build());
    }

    private CfnDomain eventSourcingElasticsearch(Vpc vpc) {
        return new CfnDomain(this, "ElasticsearchCluster",
            CfnDomainProps.builder()
                .domainName("eventsourcing")
                .elasticsearchClusterConfig(CfnDomain.ElasticsearchClusterConfigProperty.builder()
                    .dedicatedMasterEnabled(true)
                    .instanceCount(2)
                    .zoneAwarenessEnabled(true)
                    .instanceType("m3.medium.elasticsearch")
                    .dedicatedMasterType("m3.medium.elasticsearch")
                    .dedicatedMasterCount(3)
                    .build())
                .ebsOptions(CfnDomain.EBSOptionsProperty.builder()
                    .ebsEnabled(true)
                    .iops(0)
                    .volumeSize(20)
                    .volumeType("gp2")
                    .build())
                .snapshotOptions(CfnDomain.SnapshotOptionsProperty.builder()
                    .automatedSnapshotStartHour(0)
                    .build())
                .build());
    }
}
