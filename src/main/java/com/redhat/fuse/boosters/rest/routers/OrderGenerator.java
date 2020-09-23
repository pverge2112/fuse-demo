package com.redhat.fuse.boosters.rest.routers.lab01;

import org.apache.camel.builder.RouteBuilder;

import org.springframework.stereotype.Component;

import com.redhat.fuse.boosters.rest.service.OrderService;

@Component
public class OrderGenerator extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Generate 5 random orders in one second.
        from("timer:generate?repeatCount=5&period=1000")
            .routeId("generate-orders")
            .log("Generating Order...")
            .bean(OrderService.class,"generateOrder")
            .log("Order ${body.item} generated")
            .to("direct:book-to-file");

        // Content based routing based oo item, saved to file system, transformed to either xml or json
        from("direct:book-to-file")
            .routeId("book-to-file")
            .choice()
                .when(simple("${body.item} == 'Camel'"))
                    .log("Processing a Camel book")
                    .process(new OrderProcessor())
                    .marshal().json()
                    .to("file:/tmp/fuse-workshop/camel?fileName=camel-${date:now:yyyy-MM-dd-HHmmssSSS}.json")
                .otherwise()
                    .log("Processing an ActiveMQ book")
                    .process(new OrderProcessor())
                    .marshal().jacksonxml()
                    .to("file:/tmp/fuse-workshop/activemq?fileName=activemq-${date:now:yyyy-MM-dd-HHmmssSSS}.xml");

        // Orders are processed and sent to an sftp server
        from("file:/tmp/fuse-workshop/camel?delete=true")
            .routeId("camel-to-sftp")
            .log("Uploading camel orders to ftp")
        .to("sftp://localhost:23/fuse-demo?username=foo&password=pass&useUserKnownHostsFile=false&passiveMode=true&disconnect=true");


        from("file:/tmp/fuse-workshop/activemq?delete=true")
        .routeId("activemq-to-sftp")
            .log("Uploading activemq orders to ftp")
        .to("sftp://localhost:23/fuse-demo?username=foo&password=pass&useUserKnownHostsFile=false&passiveMode=true&disconnect=true");

    }

}
